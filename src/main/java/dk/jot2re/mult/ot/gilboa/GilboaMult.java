package dk.jot2re.mult.ot.gilboa;

import dk.jot2re.mult.AbstractAdditiveMult;
import dk.jot2re.mult.IntegerShare;
import dk.jot2re.mult.ot.ot.otextension.OtExtensionResourcePool;
import dk.jot2re.mult.ot.ot.otextension.RotFactory;
import dk.jot2re.mult.ot.util.ExceptionConverter;
import dk.jot2re.mult.ot.util.StrictBitVector;
import dk.jot2re.network.INetwork;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.List;

import static dk.jot2re.mult.ot.DefaultOTParameters.DEFAULT_BATCH_SIZE;
import static dk.jot2re.mult.ot.util.Fiddling.ceil;


public class GilboaMult extends AbstractAdditiveMult {
    private final OtExtensionResourcePool resources;
    private final boolean safeExpansion;
    private final int adjustedBatchSize;
    private GilboaOTFactory factory;
    private int upperBound;
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
        byte[] newSeed = new byte[ceil(resources.getLambdaSecurityParam(), 8)];
        resources.getRandomGenerator().nextBytes(newSeed);
        SecureRandom rnd = ExceptionConverter.safe(()-> SecureRandom.getInstance("SHA1PRNG"), "Randomness algorithm does not exist");
        rnd.setSeed(newSeed);
        super.rand = rnd;
        this.network = network;
    }

    @Override
    public BigInteger mult(BigInteger shareA, BigInteger shareB, BigInteger modulo) {
        return multShares(new IntegerShare(shareA), new IntegerShare(shareB), modulo).getRawShare();
    }

    @Override
    public BigInteger mult(BigInteger shareA, BigInteger shareB, BigInteger modulo, int upperBound) {
        if (upperBound > modulo.bitLength()) {
            throw new RuntimeException("Upper bound larger than modulo");
        }
        this.upperBound = upperBound;
        this.amountBytes = modulo.bitLength() / 8;
        if (safeExpansion) {
            this.expansionSizeBytes = amountBytes + ceil(resources.getLambdaSecurityParam(), 8);
        } else {
            this.expansionSizeBytes = amountBytes;
        }
        return internalMult(new IntegerShare(shareA), new IntegerShare(shareB), modulo).getRawShare();
    }

    public IntegerShare multShares(IntegerShare left, IntegerShare right, BigInteger modulo) {
        this.upperBound = modulo.bitLength();
        this.amountBytes = modulo.bitLength() / 8;
        if (safeExpansion) {
            this.expansionSizeBytes = amountBytes + ceil(resources.getLambdaSecurityParam(), 8);
        } else {
            this.expansionSizeBytes = amountBytes;
        }
        return internalMult(left, right, modulo);
    }

    private IntegerShare internalMult(IntegerShare left, IntegerShare right, BigInteger modulo) {
        BigInteger shareA = left.getRawShare();
        BigInteger shareB = right.getRawShare();
        BigInteger senderShare, receiverShare;
        if (resources.getMyId() == 0) {
            senderShare = senderRole(shareA, modulo, upperBound);
            receiverShare = receiverRole(shareB, modulo);
        } else {
            receiverShare = receiverRole(shareB, modulo);
            senderShare = senderRole(shareA, modulo, upperBound);
        }
        BigInteger res = receiverShare.add(senderShare).add(shareA.multiply(shareB)).mod(modulo);
        return new IntegerShare(res);
    }

    private BigInteger senderRole(BigInteger value, BigInteger modulo, int uppperBound) {
        List<BigInteger> shares = factory.send(value, modulo, uppperBound);
        BigInteger z = BigInteger.ZERO;
        for (int i = 0; i < upperBound; i++) {
            z = z.add(shares.get(i).multiply(BigInteger.ONE.shiftLeft(i))).mod(modulo);
        }
        z = z.negate().mod(modulo);
        return z;
    }

    private BigInteger receiverRole(BigInteger value, BigInteger modulo) {
        StrictBitVector choice = choices(value, upperBound);
        List<BigInteger> shares = factory.receive(choice, amountBytes);
        BigInteger z = BigInteger.ZERO;
        for (int i = 0; i < upperBound; i++) {
            z = z.add(shares.get(i).multiply(BigInteger.ONE.shiftLeft(i))).mod(modulo);
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
