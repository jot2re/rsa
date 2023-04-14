package dk.jot2re.mult.ot.ips;

import dk.jot2re.mult.IMult;
import dk.jot2re.mult.ot.OTMultResourcePool;
import dk.jot2re.mult.ot.ot.otextension.RotFactory;
import dk.jot2re.network.INetwork;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static dk.jot2re.mult.ot.DefaultOTParameters.DEFAULT_BATCH_SIZE;
import static dk.jot2re.mult.ot.util.Fiddling.ceil;


public class IPSMult implements IMult {
    private final OTMultResourcePool resources;
    private final boolean safeExpansion;
    private final int adjustedBatchSize;
    private INetwork network;
    private Map<Integer, IPSOTFactory> factory;
    private int amountBits;
    private int expansionSizeBytes;

    /**
     * If safeExpansion is false, then it is required that the modulo is exponentially close to a two-power.
     * batchSize must be a 2-power
     */
    public IPSMult(OTMultResourcePool resources, int batchSize, boolean safeExpansion) {
        this.resources = resources;
        this.safeExpansion = safeExpansion;
        this.adjustedBatchSize = batchSize - resources.getCompSec()- resources.getStatSec();
    }

    public IPSMult(OTMultResourcePool resources) {
        this.resources = resources;
        this.safeExpansion = true;
        this.adjustedBatchSize = DEFAULT_BATCH_SIZE - resources.getCompSec()- resources.getStatSec();
    }

    @Override
    public void init(INetwork network) {
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
        this.network = network;
    }

    @Override
    public BigInteger mult(BigInteger shareA, BigInteger shareB, BigInteger modulo) {
        this.amountBits = resources.getCompSec()+resources.getStatSec();
        if (safeExpansion) {
            this.expansionSizeBytes = (modulo.bitLength()/8) + ceil(resources.getStatSec(), 8);
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
