package anonymous.compiler;

import anonymous.network.INetwork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class CompiledProtocol {
    private static final Logger logger = LoggerFactory.getLogger(CompiledProtocol.class);
    private INetwork network;
    private NetworkPair networks;
    private Random random;
    private int parties;
    private final CompiledProtocolResources resources;
    private final ICompilableProtocol internalProtocolBrain;
    private final ICompilableProtocol internalProtocolPinky;
    public CompiledProtocol(CompiledProtocolResources resources, ICompilableProtocol internalProtocolBrain, ICompilableProtocol internalProtocolPinky) {
        this.resources = resources;
        this.internalProtocolBrain = internalProtocolBrain;
        this.internalProtocolPinky = internalProtocolPinky;
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
            SecureRandom myBrainRand = SecureRandom.getInstance("SHA1PRNG", "SUN");
            internalProtocolBrain.init(networks.getBrainNetwork(), myBrainRand);

            networks.getPinkyNetwork().init();
            byte[] myPinkySeed = network.receive(BaseNetwork.getMyVirtualPinkyId(network));
            SecureRandom myPinkyRand = SecureRandom.getInstance("SHA1PRNG", "SUN");
            myPinkyRand.setSeed(myPinkySeed);
            internalProtocolPinky.init(networks.getPinkyNetwork(), myPinkyRand);
        } catch (Exception e) {
            throw new RuntimeException("Party " + networks.getBrainNetwork().myId() + " with error " +e.getMessage());
        }
    }

    public List<BigInteger> execute(List<BigInteger> privateInput, List<BigInteger> publicInput) {
        try {
            ArrayList<BigInteger> adjustedInput = input(privateInput);
            ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
            List<Future<List<BigInteger>>> compRes = new ArrayList<>(2);
            // TODO change protocol to output hidden message
            compRes.add(executor.submit(() ->internalProtocolBrain.executeList(adjustedInput, publicInput)));
            compRes.add(executor.submit(() ->internalProtocolPinky.executeList(adjustedInput, publicInput)));
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
            BigInteger res = BigInteger.ONE;
            if (!check()) {
                logger.error("Check did not pass");
                res = BigInteger.ZERO;
//                throw new MaliciousException("Corruption happened");
            }
            if (!lastMsg(compRes.get(0).get(), compRes.get(1).get())) {
                logger.error("Last msg did not pass");
                res = BigInteger.ZERO;
                //                throw new MaliciousException("Corruption happened");
            }
            return Arrays.asList(res);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    private ArrayList<BigInteger> input(List<BigInteger> privateInput) {
        Map<Integer, ArrayList<BigInteger>> sharedInput = shareInput(privateInput);
        ArrayList<BigInteger> actualInputs = sharedInput.get(network.myId());
        for (int i: sharedInput.keySet()) {
                broadcast(i, sharedInput.get(i));
                ArrayList<BigInteger> recievedShares = receiveBroadcast(i);
                for (int j = 0; j < recievedShares.size(); j++) {
                    actualInputs.set(j, actualInputs.get(j).add(recievedShares.get(j)));
                }
        }
        return actualInputs;
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
        if (BaseNetwork.getSubmissivePinkyId(sendingParty, parties) != network.myId()) {
            pinkyRes = network.receive(BaseNetwork.getSubmissivePinkyId(sendingParty, parties));
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

    protected Map<Integer, ArrayList<BigInteger>> shareInput(List<BigInteger> input) {
        Map<Integer, ArrayList<BigInteger>> res = new HashMap<>(parties);
        for (int j = 0; j < parties; j++) {
            res.put(j, new ArrayList<>(input.size()));
        }
        for (int i = 0; i < input.size(); i++) {
            BigInteger randomSum = BigInteger.ZERO;
            for (int j = 0; j < parties-1; j++) {
                // todo should be statsec+log parties
                BigInteger share = new BigInteger(input.get(i).bitLength()+ resources.getCompSec()+parties, random);
                randomSum = randomSum.add(share);
                res.get(j).add(share);
            }
            // Compute the share of the pivot party
            res.get(parties-1).add(input.get(i).subtract(randomSum));
        }
        return res;
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
    protected boolean lastMsg(List<BigInteger> brainResult, List<BigInteger> pinkyResult) {
        boolean res = true;
        if (brainResult.size() != pinkyResult.size()) {
            res = false;
            logger.error("Different size output of the protocols");
        }
        for (int i = 0; i < brainResult.size(); i++) {
            if (!brainResult.get(i).equals(pinkyResult.get(i))) {
                res = false;
                logger.error("Inconsistent results");
            }
        }
        try {
            networks.getBrainNetwork().sendToAll(brainResult.get(0));
            networks.getPinkyNetwork().sendToAll(pinkyResult.get(0));
            Map<Integer, BigInteger> otherBrainShares = networks.getBrainNetwork().receiveFromAllPeers();
            Map<Integer, BigInteger> otherPinkyShares = networks.getPinkyNetwork().receiveFromAllPeers();
            BigInteger brainRes = otherBrainShares.values().stream().reduce(brainResult.get(0), (a, b) -> a.add(b));
            BigInteger pinkyRes = otherPinkyShares.values().stream().reduce(pinkyResult.get(0), (a, b) -> a.add(b));
            if (!brainRes.equals(pinkyRes)) {
                res = false;
                logger.error("Inconsistent pinky and brain received results");
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not send last result");
        }
        return res;
    }

}
