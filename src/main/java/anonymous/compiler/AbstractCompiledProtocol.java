package anonymous.compiler;

import anonymous.network.INetwork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
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

    protected void broadcast(int virtualId, ArrayList<BigInteger> value) {
        if (virtualId != network.myId()) {
            network.send(virtualId, value);
        }
        if (BaseNetwork.getSubmissivePinkyId(virtualId, parties) != network.myId()) {
            network.send(BaseNetwork.getSubmissivePinkyId(virtualId, parties), value);
        }
    }

    protected ArrayList<BigInteger> receiveBroadcast(int sendingParty) {
        // todo just send a hash instead
        ArrayList<BigInteger> res = null;
        if (sendingParty != network.myId()) {
            res = network.receive(sendingParty);
        }
        ArrayList<BigInteger> pinkyRes = null;
        if (BaseNetwork.getMyVirtualPinkyId(sendingParty, parties) != network.myId()) {
            pinkyRes = network.receive(BaseNetwork.getMyVirtualPinkyId(sendingParty, parties));
        }
        if (res != null && pinkyRes != null && !res.equals(pinkyRes)) {
            logger.error("Bad broadcast");
//            throw new RuntimeException("bad broadcast");
        }
        if (res != null) {
            return res;
        }
        return pinkyRes;
    }


    protected boolean check() {
        byte[] brainDigest = networks.getBrainNetwork().getDigestRes();
        byte[] pinkyDigest = networks.getPinkyNetwork().getDigestRes();
        if (!Arrays.equals(brainDigest, pinkyDigest)) {
            logger.error("Different digests");
            return false;
        }
        return true;
    }

}
