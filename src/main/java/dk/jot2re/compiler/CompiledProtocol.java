package dk.jot2re.compiler;

import dk.jot2re.network.INetwork;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.util.List;
import java.util.Random;

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

    public void init(INetwork network, Random random) throws NoSuchAlgorithmException, NoSuchProviderException {
        this.network = network;
        this.random = random;
        byte[] subPinkySeed = new byte[resources.getCompSecBytes()];
        random.nextBytes(subPinkySeed);
        this.network.send(BrainNetwork.getSubmissivePinkyId(network), subPinkySeed);
        INetwork compiledBrainNetwork = new BrainNetwork(network);
        compiledBrainNetwork.init();
        SecureRandom myBrainRand = SecureRandom.getInstance("SHA1PRNG", "SUN");
        internalProtocolBrain.init(compiledBrainNetwork, myBrainRand);
        byte[] myPinkySeed = this.network.receive(BrainNetwork.getMyVirtualPinkyId(network));
        SecureRandom myPinkyRand = SecureRandom.getInstance("SHA1PRNG", "SUN");
        myPinkyRand.setSeed(myPinkySeed);
        INetwork compiledPinkyNetwork = new PinkyNetwork(network);
        compiledPinkyNetwork.init();
        internalProtocolPinky.init(compiledPinkyNetwork, myPinkyRand);
    }

    public List<BigInteger> execute(List<BigInteger> privateInput, List<BigInteger> publicInput) {
        return check(internalProtocolBrain.executeList(privateInput, publicInput),
                internalProtocolPinky.executeList(privateInput, publicInput));
    }

    protected List<BigInteger> check(List<BigInteger> brainResult, List<BigInteger> pinkyResult) {
        // TODO check
        return brainResult;
    }

}
