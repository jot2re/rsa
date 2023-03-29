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
    private INetwork network;
    private RotSender sender;
    private RotReceiver receiver;

    public GilboaMult(OtExtensionResourcePool resources) {
        this.resources = resources;
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
        BigInteger senderShare, receiverShare;
        if (resources.getMyId() == 0) {
            senderShare = senderRole(shareA, modulo);
            receiverShare = receiverRole(shareB, modulo);
        } else {
            receiverShare = receiverRole(shareA, modulo);
            senderShare = senderRole(shareB, modulo);
        }
        return receiverShare.add(senderShare).add(shareA.multiply(shareB)).mod(modulo);
    }

    private BigInteger senderRole(BigInteger value, BigInteger modulo) {
        // todo check amount size is divisible by 8 and after that a two power
        int amountBits = modulo.bitLength();
        int amountBytes = amountBits/8;
        int expansionSizeBytes = amountBytes + ceil(resources.getLambdaSecurityParam(), 8);
        Pair<List<StrictBitVector>, List<StrictBitVector>> seedRes = sender.extend(amountBits);
        List<byte[]> expandedZero = new ArrayList<>(amountBits);
        ArrayList<byte[]> correction = new ArrayList<>(amountBits);
        BigInteger z = BigInteger.ZERO;
        for (int i = 0; i < amountBits; i++) {
            byte[] curZero = expand(seedRes.getFirst().get(i), expansionSizeBytes);
            BigInteger curZeroVal = new BigInteger(1, curZero).mod(modulo);
            BigInteger actualOneVal = curZeroVal.add(value).mod(modulo);
            byte[] oneBytes = actualOneVal.toByteArray();
            byte[] actualOneBytes = new byte[amountBytes];
            for (int j = 0; j < amountBytes; j++) {
                if (oneBytes.length > j) {
                    actualOneBytes[actualOneBytes.length-j-1] = oneBytes[oneBytes.length-j-1];
                }
            }
            byte[] curOne = expand(seedRes.getSecond().get(i), amountBytes);
            ByteArrayHelper.xor(curOne, actualOneBytes);
            correction.add(curOne);
            expandedZero.add(curZero);
            z = z.add(curZeroVal.multiply(BigInteger.TWO.pow(i))).mod(modulo);
        }
        z = z.negate().mod(modulo);
        network.send(resources.getOtherId(), correction);
        return z;
    }

    private BigInteger receiverRole(BigInteger value, BigInteger modulo) {
        int amountBits = modulo.bitLength();
        int amountBytes = amountBits/8;
        int expansionSizeBytes = amountBytes + ceil(resources.getLambdaSecurityParam(), 8);
        StrictBitVector choice = choices(value, amountBits);
        List<StrictBitVector> results = receiver.extend(choice);
        ArrayList<byte[]> correction = network.receive(resources.getOtherId());
        List<byte[]> expandedBytes = new ArrayList<>(amountBits);
        List<BigInteger> expandedVals = new ArrayList<>(amountBits);
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
            BigInteger curVal = new BigInteger(1, curExpanded).mod(modulo);
            z = z.add(curVal.multiply(BigInteger.TWO.pow(i))).mod(modulo);
            expandedVals.add(curVal);
            expandedBytes.add(curExpanded);
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
