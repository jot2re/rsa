package dk.jot2re.mult.ot.gilboa;

import dk.jot2re.mult.AbstractAdditiveMult;
import dk.jot2re.mult.ot.ot.otextension.OtExtensionResourcePool;
import dk.jot2re.mult.ot.ot.otextension.RotFactory;
import dk.jot2re.mult.ot.util.StrictBitVector;
import dk.jot2re.network.INetwork;

import java.math.BigInteger;
import java.util.List;

import static dk.jot2re.mult.ot.DefaultOTParameters.DEFAULT_BATCH_SIZE;
import static dk.jot2re.mult.ot.util.Fiddling.ceil;


public class GilboaMult extends AbstractAdditiveMult {
    private final OtExtensionResourcePool resources;
    private final boolean safeExpansion;
    private final int adjustedBatchSize;
    private INetwork network;
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
        this.network = network;
    }

    @Override
    public BigInteger mult(BigInteger shareA, BigInteger shareB, BigInteger modulo, int upperBound) {
        if (upperBound > modulo.bitLength()) {
            throw new RuntimeException("Upper bound larger than modulo");
        }
        this.upperBound = upperBound;
        return internalMult(shareA, shareB, modulo);
    }

    public BigInteger multShares(BigInteger left, BigInteger right, BigInteger modulo) {
        this.upperBound = modulo.bitLength();
        return internalMult(left, right, modulo);
    }

    private BigInteger internalMult(BigInteger left, BigInteger right, BigInteger modulo) {
        this.amountBytes = modulo.bitLength() / 8;
        if (safeExpansion) {
            this.expansionSizeBytes = amountBytes + ceil(resources.getLambdaSecurityParam(), 8);
        } else {
            this.expansionSizeBytes = amountBytes;
        }
        BigInteger senderShare, receiverShare;
        if (resources.getMyId() == 0) {
            senderShare = senderRole(left, modulo, upperBound);
            receiverShare = receiverRole(right, modulo);
        } else {
            receiverShare = receiverRole(right, modulo);
            senderShare = senderRole(left, modulo, upperBound);
        }
        BigInteger res = receiverShare.add(senderShare).add(left.multiply(right)).mod(modulo);
        return res;
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
