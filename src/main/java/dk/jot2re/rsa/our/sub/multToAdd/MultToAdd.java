package dk.jot2re.rsa.our.sub.multToAdd;

import dk.jot2re.AbstractProtocol;
import dk.jot2re.network.INetwork;
import dk.jot2re.network.NetworkException;
import dk.jot2re.rsa.bf.BFParameters;
import dk.jot2re.rsa.our.RSAUtil;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class MultToAdd extends AbstractProtocol {
    private final BFParameters params;
    private boolean initialized = false;

    public MultToAdd(BFParameters params) {
        this.params = params;
    }

    @Override
    public void init(INetwork network, Random random) {
        if (!initialized) {
            super.init(network, random);
            params.getMult().init(network, random);
            initialized = true;
        }
    }

    /**
     * Executes without making correlated randomness first.
     */
    public BigInteger execute(BigInteger x, BigInteger modulo) throws NetworkException {
        Map<Integer, Serializable> multShares = new HashMap<>(network.getNoOfParties());
        multShares.put(network.myId(), params.getMult().share(x, modulo));
        for (int party : network.peers()) {
            multShares.put(party, params.getMult().share(party, modulo));
        }
        return params.getMult().combineToAdditive(RSAUtil.multList(params, multShares.values().toArray(new Serializable[0]), modulo), modulo);
    }

    public BigInteger executeWithCorrelatedRandomness(BigInteger x, BigInteger modulo) throws NetworkException {
        BigInteger rand = RSAUtil.sample(random, modulo);
        BigInteger additiveRShare = execute(rand, modulo);
        BigInteger paddedShare = rand.modInverse(modulo).multiply(x).mod(modulo);
        network.sendToAll(paddedShare);
        Map<Integer, BigInteger> otherPaddings = network.receiveFromAllPeers();
        BigInteger combinedPadding = otherPaddings.values().stream().reduce(paddedShare, (a,b) -> a.multiply(b).mod(modulo));
        return combinedPadding.multiply(additiveRShare).mod(modulo);
    }

    protected Map<Integer, BigInteger> inverse(BigInteger val, BigInteger modulo) {
        Map<Integer, BigInteger> shares = new HashMap<>(network.getNoOfParties());
        BigInteger sum = BigInteger.ZERO;
        for (int i : network.peers()) {
            BigInteger sampled = RSAUtil.sample(random, modulo);
            shares.put(i, sampled);
            sum = sum.add(sampled).mod(modulo);
        }
        BigInteger myShare = val.modInverse(modulo).subtract(sum).mod(modulo);
        shares.put(network.myId(), myShare);
        return shares;
    }
}
