package anonymous.compiler;

import anonymous.network.INetwork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.security.SecureRandom;
import java.util.*;

public abstract class AbstractCompiledProtocol {
    private static final Logger logger = LoggerFactory.getLogger(AbstractCompiledProtocol.class);
    protected INetwork network;
    protected NetworkPair networks;
    protected Random random;
    protected SecureRandom brainRand;
    protected SecureRandom pinkyRand;
    protected int parties;
    protected final CompiledProtocolResources resources;
    public AbstractCompiledProtocol(CompiledProtocolResources resources) {
        this.resources = resources;
    }

    public void init(NetworkPair networks, Random random) {
        try {
            this.parties = networks.getBrainNetwork().getNoOfParties();
            this.network = networks.getBrainNetwork().internalNetwork;
            this.networks = networks;
            this.random = random;
            byte[] subPinkySeed = new byte[resources.getCompSecBytes()];
            random.nextBytes(subPinkySeed);
            networks.getBrainNetwork().init();
            network.send(BaseNetwork.getSubmissivePinkyId(network), subPinkySeed);
            brainRand = SecureRandom.getInstance("SHA1PRNG", "SUN");

            networks.getPinkyNetwork().init();
            byte[] myPinkySeed = network.receive(BaseNetwork.getMyVirtualPinkyId(network));
            pinkyRand = SecureRandom.getInstance("SHA1PRNG", "SUN");
            pinkyRand.setSeed(myPinkySeed);
        } catch (Exception e) {
            throw new RuntimeException("Party " + networks.getBrainNetwork().myId() + " with error " +e.getMessage());
        }
    }

    protected boolean check() {
        for (int i = 0; i < network.getNoOfParties(); i++) {
            if (i != network.myId()) {
                network.send(i, networks.getBrainNetwork().getDigestSnd(i));
            }
            if (BaseNetwork.getMyVirtualPinkyId(i, parties) != network.myId()) {
                network.send(BaseNetwork.getMyVirtualPinkyId(i, parties), networks.getPinkyNetwork().getDigestSnd(i));
            }
        }
        byte[] recDigest = null;
        boolean res = true;
        for (int i = 0; i < network.getNoOfParties(); i++) {
            if (i != network.myId()) {
                recDigest =network.receive(i);
            }
            byte[] recPinky = null;
            if (BaseNetwork.getSubmissivePinkyId(i, parties) != network.myId()) {
                recPinky = network.receive(BaseNetwork.getSubmissivePinkyId(i, parties));
            }
            if (!Arrays.equals(recDigest, recPinky)) {
            logger.error("Different digests");
            res = false;
            }
        }
        return res;
    }

    public static void broadcastValidation(INetwork network, int sender, List<Integer> parties, Serializable data) {
        try {
            for (int i :parties) {
                if (i != sender) {
                    network.send(i, data);
                }
            }
            for (int i : parties) {
                if (i != sender) {
                    Serializable cand = network.receive(i);
                    if (!cand.equals(data)) {
                        logger.error("Inconsistent data received");
//                        throw new RuntimeException("Inconsistent data received");
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not broadcast", e);
        }
    }

}
