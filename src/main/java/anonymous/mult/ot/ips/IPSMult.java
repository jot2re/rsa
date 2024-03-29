package anonymous.mult.ot.ips;

import anonymous.mult.AbstractAdditiveMult;
import anonymous.mult.ot.DefaultOTParameters;
import anonymous.mult.ot.OTMultResourcePool;
import anonymous.mult.ot.ot.otextension.RotFactory;
import anonymous.mult.ot.util.ExceptionConverter;
import anonymous.mult.ot.util.Fiddling;
import anonymous.network.INetwork;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;


public class IPSMult extends AbstractAdditiveMult {
    private final OTMultResourcePool resources;
    private final boolean safeExpansion;
    private final int adjustedBatchSize;
    private Map<Integer, IPSOTFactory> factory;
    private int amountBits;
    private int expansionSizeBytes;

    /**
     * If safeExpansion is false, then it is required that the modulo is exponentially close to a two-power.
     * batchSize must be a 2-power
     */
    public IPSMult(OTMultResourcePool resources, int batchSize, boolean safeExpansion) {
        //todo add seed for super class
        this.resources = resources;
        this.safeExpansion = safeExpansion;
        this.adjustedBatchSize = batchSize - resources.getCompSec()- resources.getStatSec();
    }

    public IPSMult(OTMultResourcePool resources) {
        this.resources = resources;
        this.safeExpansion = true;
        this.adjustedBatchSize = DefaultOTParameters.DEFAULT_BATCH_SIZE - resources.getCompSec()- resources.getStatSec();

    }

    private Random getRandom(OTMultResourcePool resources) {
        byte[] newSeed = new byte[Fiddling.ceil(resources.getCompSec(), 8)];
        resources.getOtResources((resources.getMyId()+1) % resources.getParties()).getRandomGenerator().nextBytes(newSeed);
        SecureRandom rnd = ExceptionConverter.safe(()-> SecureRandom.getInstance("SHA1PRNG"), "Randomness algorithm does not exist");
        rnd.setSeed(newSeed);
        return rnd;
    }

    @Override
    public void init(INetwork network, Random random) {
        super.network = network;
        super.random = random;//getRandom(resources);
        //todo fix consistency in when network is supplied
        if (this.factory == null) {
            this.factory = new HashMap<>(resources.getParties()-1);
            for (int i = 0; i < resources.getParties(); i++) {
                if (resources.getMyId() != i) {
                    RotFactory rotFactory = new RotFactory(resources.getOtResources(i), network);
                    IPSOTFactory ipsFactory = new IPSOTFactory(rotFactory, resources.getOtResources(i), network, adjustedBatchSize);
                    if (resources.getMyId() < i) {
                        ipsFactory.initSender();
                        ipsFactory.initReceiver();
                    } else {
                        ipsFactory.initReceiver();
                        ipsFactory.initSender();
                    }
                    factory.put(i, ipsFactory);
                }
            }
        }
    }

    @Override
    public BigInteger multShares(BigInteger shareA, BigInteger shareB, BigInteger modulo) {
        this.amountBits = modulo.bitLength()+resources.getStatSec();
        if (safeExpansion) {
            this.expansionSizeBytes = (modulo.bitLength()/8) + Fiddling.ceil(resources.getStatSec(), 8);
        } else {
            this.expansionSizeBytes = modulo.bitLength()/8;
        }
        BigInteger partialRes = shareA.multiply(shareB).mod(modulo);
        for (int i = 0; i < resources.getParties(); i++) {
            if (resources.getMyId() != i) {
                BigInteger senderShare, receiverShare;
                if (resources.getMyId() < i) {
                    senderShare = senderRole(factory.get(i).getSender(), shareA, modulo);
                    receiverShare = receiverRole(factory.get(i).getReceiver(), shareB, modulo);
                } else {
                    receiverShare = receiverRole(factory.get(i).getReceiver(), shareB, modulo);
                    senderShare = senderRole(factory.get(i).getSender(), shareA, modulo);
                }
                partialRes = senderShare.add(receiverShare).add(partialRes);
            }
        }
        return partialRes.mod(modulo);
    }

    private BigInteger senderRole(IPSOTSender sender, BigInteger value, BigInteger modulo) {
        List<BigInteger> shares = sender.send(value, modulo, expansionSizeBytes);
        BigInteger z = BigInteger.ZERO;
        for (int i = 0; i < amountBits; i++) {
            z = z.add(shares.get(i));
        }
        z = z.negate().mod(modulo);
        return z;
    }

    private BigInteger receiverRole(IPSOTReceiver receiver, BigInteger value, BigInteger modulo) {;
        List<BigInteger> receivedShares = receiver.receive(value, modulo, expansionSizeBytes);
        BigInteger z = BigInteger.ZERO;
        for (int i = 0; i < amountBits; i++) {
            z = z.add(receivedShares.get(i));
        }
        return z.mod(modulo);
    }
}
