package dk.jot2re.compiler;

import dk.jot2re.network.INetwork;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class CompiledProtocol {
    private INetwork network;
    private Random random;
    private final CompiledProtocolResources resources;
    private final ICompilableProtocol internalProtocolBrain;
    private final ICompilableProtocol internalProtocolPinky;
    public CompiledProtocol(CompiledProtocolResources resources, ICompilableProtocol internalProtocolBrain, ICompilableProtocol internalProtocolPinky) {
        this.resources = resources;
        this.internalProtocolBrain = internalProtocolBrain;
        this.internalProtocolPinky = internalProtocolPinky;
    }

    public void init(INetwork brainNetwork, INetwork pinkyNetwork, Random random) {
        try {
            this.network = brainNetwork;
            this.random = random;
            byte[] subPinkySeed = new byte[resources.getCompSecBytes()];
            random.nextBytes(subPinkySeed);
            INetwork compiledBrainNetwork = new BrainNetwork(brainNetwork, pinkyNetwork);
            compiledBrainNetwork.init();
            network.send(CompiledNetwork.getSubmissivePinkyId(network), subPinkySeed);
            SecureRandom myBrainRand = SecureRandom.getInstance("SHA1PRNG", "SUN");
            internalProtocolBrain.init(compiledBrainNetwork, myBrainRand);

            INetwork compiledPinkyNetwork = new PinkyNetwork(pinkyNetwork);
            compiledPinkyNetwork.init();
            byte[] myPinkySeed = network.receive(CompiledNetwork.getMyVirtualPinkyId(network));
            SecureRandom myPinkyRand = SecureRandom.getInstance("SHA1PRNG", "SUN");
            myPinkyRand.setSeed(myPinkySeed);
            internalProtocolPinky.init(compiledPinkyNetwork, myPinkyRand);
        } catch (Exception e) {
            throw new RuntimeException("Party " + network.myId() + " with error " +e.getMessage());
        }
    }

    public List<BigInteger> execute(List<BigInteger> privateInput, List<BigInteger> publicInput) {
        try {
            ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
            List<Future<List<BigInteger>>> res = new ArrayList<>(2);
            res.add(executor.submit(() ->internalProtocolBrain.executeList(privateInput, publicInput)));
            res.add(executor.submit(() ->internalProtocolPinky.executeList(privateInput, publicInput)));
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
            return check(res.get(0).get(), res.get(1).get());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected List<BigInteger> check(List<BigInteger> brainResult, List<BigInteger> pinkyResult) {
        // TODO check
        return brainResult;
    }

}
