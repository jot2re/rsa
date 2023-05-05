package anonymous.compiler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.*;

public class CompiledProtocol extends AbstractCompiledProtocol {
    private static final Logger logger = LoggerFactory.getLogger(CompiledProtocol.class);
    private final ICompilableProtocol internalProtocolBrain;
    private final ICompilableProtocol internalProtocolPinky;
    public CompiledProtocol(CompiledProtocolResources resources, ICompilableProtocol internalProtocolBrain, ICompilableProtocol internalProtocolPinky) {
        super(resources);
        this.internalProtocolBrain = internalProtocolBrain;
        this.internalProtocolPinky = internalProtocolPinky;
    }

    public void init(NetworkPair networks, Random random) {
        super.init(networks, random);
        internalProtocolBrain.init(networks.getBrainNetwork(), brainRand);
        internalProtocolPinky.init(networks.getPinkyNetwork(), pinkyRand);
    }

    public List<BigInteger> execute(List<BigInteger> privateInput, List<BigInteger> publicInput) {
        try {
            ArrayList<BigInteger> adjustedInput = input(privateInput);
            ArrayList<BigInteger> brainRes = (ArrayList<BigInteger>) internalProtocolBrain.executeList(adjustedInput, publicInput);
            ArrayList<BigInteger> pinkyRes = (ArrayList<BigInteger>) internalProtocolPinky.executeList(adjustedInput, publicInput);

            BigInteger res = BigInteger.ONE;
            if (!check()) {
//                logger.error("Check did not pass");
                res = BigInteger.ZERO;
//                throw new MaliciousException("Corruption happened");
            }
            if (!lastMsg(pinkyRes, brainRes)) {
//                logger.error("Last msg did not pass");
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
        Map<Integer, ArrayList<BigInteger>> toValidate = new HashMap<>();
        for (int i: sharedInput.keySet()) {
            if (i != network.myId()) {
                network.send(i, sharedInput.get(i));
            }
        }
        for (int i: network.peers()) {
            ArrayList<BigInteger> recievedShares = network.receive(i);
                for (int j = 0; j < recievedShares.size(); j++) {
                    actualInputs.set(j, actualInputs.get(j).add(recievedShares.get(j)));
                }
        }
        for (int i : toValidate.keySet()) {
            broadcastValidation(network, i, Arrays.asList(i, BaseNetwork.getSubmissivePinkyId(network)), toValidate.get(i));
        }
        return actualInputs;
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

    protected boolean lastMsg(ArrayList<BigInteger> brainResult, ArrayList<BigInteger> pinkyResult) {
//        boolean res = true;
//        if (brainResult.size() != pinkyResult.size()) {
//            res = false;
//            logger.error("Different size output of the protocols");
//        }
//        for (int i = 0; i < brainResult.size(); i++) {
//            if (!brainResult.get(i).equals(pinkyResult.get(i))) {
//                res = false;
//                logger.error("Inconsistent results");
//            }
//        }
            for (int i = 0; i < network.getNoOfParties(); i++) {
                if (i != network.myId()) {
                    network.send(i, brainResult);
                }
                if (BaseNetwork.getMyVirtualPinkyId(i, parties) != network.myId()) {
                    network.send(BaseNetwork.getMyVirtualPinkyId(i, parties), pinkyResult);
                }
            }
            ArrayList<BigInteger> recDigest = null;
            boolean res = true;
            for (int i = 0; i < network.getNoOfParties(); i++) {
                if (i != network.myId()) {
                    recDigest = network.receive(i);
                }
                ArrayList<BigInteger> recPinky = null;
                if (BaseNetwork.getSubmissivePinkyId(i, parties) != network.myId()) {
                    recPinky = network.receive(BaseNetwork.getSubmissivePinkyId(i, parties));
                }
                if (recDigest != null & recPinky != null && !recDigest.equals(recPinky)) {
                    logger.error("Inconsistent pinky and brain received results");
                    res = false;
                }
            }
            return res;
    }

}
