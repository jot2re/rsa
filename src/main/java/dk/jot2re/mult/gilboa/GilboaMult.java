package dk.jot2re.mult.gilboa;

import dk.jot2re.mult.IMult;
import dk.jot2re.mult.gilboa.ot.otextension.*;
import dk.jot2re.mult.gilboa.util.*;
import dk.jot2re.network.INetwork;

import java.math.BigInteger;

import static dk.jot2re.mult.gilboa.util.Fiddling.ceil;


public class GilboaMult implements IMult {
    private static final int DEFAULT_BATCH_SIZE = 1048576;
    private final OtExtensionResourcePool resources;
    private final boolean safeExpansion;
    private final int adjustedBatchSize;
    private INetwork network;
    private GilboaOTFactory factory;
    private int amountBits;
    private int amountBytes;
    private int expansionSizeBytes;

    /**
     * If safeExpansion is false, then it is required that the modulo is exponentially close to a two-power.
     * batchSize must be a 2-power
     */
    public GilboaMult(OtExtensionResourcePool resources, int batchSize, boolean safeExpansion) {
        this.resources = resources;
        this.safeExpansion = safeExpansion;
        this.adjustedBatchSize = batchSize - resources.getComputationalSecurityParameter()- resources.getLambdaSecurityParam();
    }

    public GilboaMult(OtExtensionResourcePool resources) {
        this.resources = resources;
        this.safeExpansion = true;
        this.adjustedBatchSize = DEFAULT_BATCH_SIZE - resources.getComputationalSecurityParameter()- resources.getLambdaSecurityParam();
    }

    @Override
    public void init(INetwork network) {
        if (this.factory == null) {
            RotFactory rotFactory = new RotFactory(resources, network);
            this.factory = new GilboaOTFactory(rotFactory, resources, network, adjustedBatchSize);
            if (resources.getMyId() == 0) {
                this.factory.initSender();
                this.factory.initReceiver();
            } else {
                this.factory.initReceiver();
                this.factory.initSender();
            }
        }
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
//        Pair<List<StrictBitVector>, List<StrictBitVector>> seedRes = sender.extend(amountBits);
//        ArrayList<byte[]> correction = new ArrayList<>(amountBits);
        BigInteger z = BigInteger.ZERO;
        for (int i = 0; i < amountBits; i++) {
            BigInteger curZeroVal = factory.send(value, modulo);
            z = z.add(curZeroVal.multiply(BigInteger.ONE.shiftLeft(i))).mod(modulo);
        }
        z = z.negate().mod(modulo);
//        network.send(resources.getOtherId(), correction);
        return z;
    }

    private BigInteger receiverRole(BigInteger value, BigInteger modulo) {
        StrictBitVector choice = choices(value, amountBits);
//        ArrayList<byte[]> correction = network.receive(resources.getOtherId());
        BigInteger z = BigInteger.ZERO;
        for (int i = 0; i < amountBits; i++) {
            BigInteger curVal = factory.receive(choice.getBit(i, false), amountBytes);
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
}
