package dk.jot2re.mult.gilboa;

import dk.jot2re.mult.IMult;
import dk.jot2re.mult.gilboa.ot.otextension.OtExtensionResourcePool;
import dk.jot2re.mult.gilboa.ot.otextension.RotFactory;
import dk.jot2re.mult.gilboa.ot.otextension.RotReceiver;
import dk.jot2re.mult.gilboa.ot.otextension.RotSender;
import dk.jot2re.mult.gilboa.util.*;
import dk.jot2re.network.INetwork;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static dk.jot2re.mult.gilboa.util.Fiddling.ceil;


public class GilboaMult implements IMult {
    private final OtExtensionResourcePool resources;
    private final boolean safeExpansion;
    private INetwork network;
    private RotSender sender;
    private RotReceiver receiver;
    private int amountBits;
    private int amountBytes;
    private int expansionSizeBytes;

    /**
     * If safeExpansion is false, then it is required that the modulo is exponentially close to a two-power.
     */
    public GilboaMult(OtExtensionResourcePool resources, boolean safeExpansion) {
        this.resources = resources;
        this.safeExpansion = safeExpansion;
    }

    public GilboaMult(OtExtensionResourcePool resources) {
        this.resources = resources;
        this.safeExpansion = true;
    }

    @Override
    public void init(INetwork network) {
        RotFactory factory = new RotFactory(resources, network);
        this.sender = factory.createSender();
        this.receiver = factory.createReceiver();
        this.network = network;
    }

    @Override
    public BigInteger mult(BigInteger shareA, BigInteger shareB, BigInteger modulo) {
        this.amountBits = modulo.bitLength();
        this.amountBytes = amountBits/8;
        if (safeExpansion) {
            this.expansionSizeBytes = amountBytes + ceil(resources.getLambdaSecurityParam(), 8);
        } else {
            this.expansionSizeBytes = amountBytes;
        }
        BigInteger senderShare, receiverShare;
        if (resources.getMyId() == 0) {
            senderShare = senderRole(shareA, modulo);
            receiverShare = receiverRole(shareB, modulo);
        } else {
            receiverShare = receiverRole(shareB, modulo);
            senderShare = senderRole(shareA, modulo);
        }
        BigInteger res = receiverShare.add(senderShare).add(shareA.multiply(shareB)).mod(modulo);
        return res;
    }

    private BigInteger senderRole(BigInteger value, BigInteger modulo) {
        Pair<List<StrictBitVector>, List<StrictBitVector>> seedRes = sender.extend(amountBits);
        ArrayList<byte[]> correction = new ArrayList<>(amountBits);
        BigInteger z = BigInteger.ZERO;
        for (int i = 0; i < amountBits; i++) {
            byte[] curZero = expand(seedRes.getFirst().get(i), expansionSizeBytes);
            BigInteger curZeroVal = new BigInteger(1, curZero);
            BigInteger actualOneVal = curZeroVal.add(value).mod(modulo);
            byte[] actualOneBytes = moveToArray(actualOneVal, amountBytes);
            byte[] curOne = expand(seedRes.getSecond().get(i), amountBytes);
            ByteArrayHelper.xor(curOne, actualOneBytes);
            correction.add(curOne);
            z = z.add(curZeroVal.multiply(BigInteger.ONE.shiftLeft(i))).mod(modulo);
        }
        z = z.negate().mod(modulo);
        network.send(resources.getOtherId(), correction);
        return z;
    }

    /**
     * Takes a UNSIGNED BigInteger and moves it into a byte array of certain size.
     * Note that no safety checks are performed of whether there is space enough
     */
    private byte[] moveToArray(BigInteger value, int bytes) {
        byte[] valueBytes = value.toByteArray();
        byte[] adjustedBytes = new byte[bytes];
        for (int j = 0; j < bytes; j++) {
            if (valueBytes.length > j) {
                adjustedBytes[adjustedBytes.length-j-1] = valueBytes[valueBytes.length-j-1];
            }
        }
        return adjustedBytes;
    }

    private BigInteger receiverRole(BigInteger value, BigInteger modulo) {
        StrictBitVector choice = choices(value, amountBits);
        List<StrictBitVector> results = receiver.extend(choice);
        ArrayList<byte[]> correction = network.receive(resources.getOtherId());
        BigInteger z = BigInteger.ZERO;
        for (int i = 0; i < amountBits; i++) {
            byte[] curExpanded;
            if (choice.getBit(i, false)) {
                // choice bit=1 so we must correct
                curExpanded = expand(results.get(i), amountBytes);
                ByteArrayHelper.xor(curExpanded, correction.get(i));
            } else {
                // choice bit=0 so we must expand enough and do modulo
                curExpanded = expand(results.get(i), expansionSizeBytes);
            }
            BigInteger curVal = new BigInteger(1, curExpanded);
            z = z.add(curVal.multiply(BigInteger.ONE.shiftLeft(i))).mod(modulo);
        }
        return z;
    }

    private StrictBitVector choices(BigInteger value, int bits) {
        StrictBitVector res = new StrictBitVector(bits);
        for (int i = 0; i < bits; i++) {
            int currentBit = value.shiftRight(i).and(BigInteger.ONE).intValueExact();
            res.setBit(i, currentBit == 1 ? true : false, false);
        }
        return res;
    }

    private byte[] expand(StrictBitVector inputBits, int amountBytes) {
        Drbg rng = new AesCtrDrbg(inputBits.toByteArray());
        byte[] res = new byte[amountBytes];
        rng.nextBytes(res);
        return res;
    }
}
