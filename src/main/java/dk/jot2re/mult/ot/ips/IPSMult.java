package dk.jot2re.mult.ot.ips;

import dk.jot2re.mult.IMult;
import dk.jot2re.mult.ot.ot.otextension.OtExtensionResourcePool;
import dk.jot2re.mult.ot.ot.otextension.RotFactory;
import dk.jot2re.network.INetwork;

import java.math.BigInteger;
import java.util.List;

import static dk.jot2re.mult.ot.util.Fiddling.ceil;


public class IPSMult implements IMult {
    private static final int DEFAULT_BATCH_SIZE = 1048576;
    private final OtExtensionResourcePool resources;
    private final boolean safeExpansion;
    private final int adjustedBatchSize;
    private INetwork network;
    private IPSOTFactory factory;
    private int amountBits;
    private int expansionSizeBytes;

    /**
     * If safeExpansion is false, then it is required that the modulo is exponentially close to a two-power.
     * batchSize must be a 2-power
     */
    public IPSMult(OtExtensionResourcePool resources, int batchSize, boolean safeExpansion) {
        this.resources = resources;
        this.safeExpansion = safeExpansion;
        this.adjustedBatchSize = batchSize - resources.getComputationalSecurityParameter()- resources.getLambdaSecurityParam();
    }

    public IPSMult(OtExtensionResourcePool resources) {
        this.resources = resources;
        this.safeExpansion = true;
        this.adjustedBatchSize = DEFAULT_BATCH_SIZE - resources.getComputationalSecurityParameter()- resources.getLambdaSecurityParam();
    }

    @Override
    public void init(INetwork network) {
        if (this.factory == null) {
            RotFactory rotFactory = new RotFactory(resources, network);
            this.factory = new IPSOTFactory(rotFactory, resources, network, adjustedBatchSize);
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
        this.amountBits = resources.getComputationalSecurityParameter()+resources.getLambdaSecurityParam();
        if (safeExpansion) {
            this.expansionSizeBytes = (modulo.bitLength()/8) + ceil(resources.getLambdaSecurityParam(), 8);
        } else {
            this.expansionSizeBytes = modulo.bitLength()/8;
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
        List<BigInteger> shares = factory.getSender().send(value, modulo, expansionSizeBytes);
        BigInteger z = BigInteger.ZERO;
        for (int i = 0; i < amountBits; i++) {
            z = z.add(shares.get(i));
        }
        z = z.negate().mod(modulo);
        return z;
    }

    private BigInteger receiverRole(BigInteger value, BigInteger modulo) {;
        List<BigInteger> receivedShares = factory.getReceiver().receive(value, modulo, expansionSizeBytes);
        BigInteger z = BigInteger.ZERO;
        for (int i = 0; i < amountBits; i++) {
            z = z.add(receivedShares.get(i));
        }
        return z.mod(modulo);
    }
}
