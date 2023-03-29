package dk.jot2re.mult.gilboa;

import dk.jot2re.mult.IMult;
import dk.jot2re.mult.gilboa.oracle.Prg;
import dk.jot2re.mult.gilboa.ot.otextension.OtExtensionResourcePool;
import dk.jot2re.mult.gilboa.ot.otextension.RotFactory;
import dk.jot2re.mult.gilboa.ot.otextension.RotReceiver;
import dk.jot2re.mult.gilboa.ot.otextension.RotSender;
import dk.jot2re.mult.gilboa.util.ByteArrayHelper;
import dk.jot2re.mult.gilboa.util.Pair;
import dk.jot2re.mult.gilboa.util.StrictBitVector;
import dk.jot2re.network.INetwork;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static dk.jot2re.mult.gilboa.oracle.Util.ceil;


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
        int amount = modulo.bitLength();
        int amountBytes = amount + ceil(resources.getLambdaSecurityParam(), 8);
        Pair<List<StrictBitVector>, List<StrictBitVector>> seedRes = sender.extend(amount);
        List<byte[]> expandedZero = new ArrayList<>(amount);
        ArrayList<byte[]> correction = new ArrayList<>(amount);
        BigInteger z = BigInteger.ZERO;
        for (int i = 0; i < amount; i++) {
            byte[] curZero = expand(seedRes.getFirst().get(i), amountBytes);
            BigInteger curZeroVal = new BigInteger(1, curZero).mod(modulo);
            BigInteger actualOneVal = curZeroVal.add(value).mod(modulo);
            byte[] actualOneBytes = actualOneVal.toByteArray();
            byte[] curOne = expand(seedRes.getSecond().get(i), amount);
            ByteArrayHelper.xor(curOne, actualOneBytes);
            correction.add(curOne);
            expandedZero.add(curZero);
            z.add(curZeroVal.multiply(BigInteger.TWO.pow(i))).mod(modulo);
        }
        z = z.negate().mod(modulo);
        network.send(resources.getOtherId(), correction);
        return z;
    }

    private BigInteger receiverRole(BigInteger value, BigInteger modulo) {
        int amount = modulo.bitLength();
        int amountBytes = amount + ceil(resources.getLambdaSecurityParam(), 8);
        StrictBitVector choice = choices(value, amount);
        List<StrictBitVector> results = receiver.extend(choice);
        ArrayList<byte[]> correction = network.receive(resources.getOtherId());
        List<byte[]> expandedBytes = new ArrayList<>(amount);
        List<BigInteger> expandedVals = new ArrayList<>(amount);
        BigInteger z = BigInteger.ZERO;
        for (int i = 0; i < amount; i++) {
            byte[] curExpanded;
            if (choice.getBit(i)) {
                // choice bit=1 so we must correct
                curExpanded = expand(results.get(i), amount);
                ByteArrayHelper.xor(curExpanded, correction.get(i));
            } else {
                // choice bit=0 so we must expand enough and do modulo
                curExpanded = expand(results.get(i), amountBytes);
            }
            BigInteger curVal = new BigInteger(1, curExpanded).mod(modulo);
            z.add(curVal.multiply(BigInteger.TWO.pow(i))).mod(modulo);
            expandedVals.add(curVal);
            expandedBytes.add(curExpanded);
        }
        return z;
    }

    private StrictBitVector choices(BigInteger value, int bits) {
        StrictBitVector res = new StrictBitVector(bits);
        for (int i = 0; i < bits; i++) {
            int currentBit = value.shiftRight(i).and(BigInteger.ONE).intValueExact();
            res.setBit(i, currentBit == 1 ? true : false);
        }
        return res;
    }

    private byte[] expand(StrictBitVector inputBits, int amountBytes) {
        // TODO in general don't use secure random but something more efficient
        Prg prg = new Prg(inputBits.toByteArray());
        return prg.getBytes(amountBytes);
    }
}
