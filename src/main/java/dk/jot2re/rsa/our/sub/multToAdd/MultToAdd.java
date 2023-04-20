package dk.jot2re.rsa.our.sub.multToAdd;

import dk.jot2re.network.NetworkException;
import dk.jot2re.rsa.bf.BFParameters;
import dk.jot2re.rsa.our.RSAUtil;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

public class MultToAdd {
    private final BFParameters params;

    public MultToAdd(BFParameters params) {
        this.params = params;
    }

    /**
     * Executes without making correlated randomness first.
     */
    public BigInteger execute(BigInteger x, BigInteger modulo) throws NetworkException {
        Map<Integer, Serializable> multShares = new HashMap<>(params.getAmountOfPeers()+1);
        multShares.put(params.getMyId(), params.getMult().share(x, modulo));
        for (int party : params.getNetwork().peers()) {
            multShares.put(party, params.getMult().share(party, modulo));
        }
        return params.getMult().combineToAdditive(RSAUtil.multList(params, multShares.values().toArray(new Serializable[0]), modulo), modulo);
    }

    public BigInteger executeWithCorrelatedRandomness(BigInteger x, BigInteger modulo) throws NetworkException {
        BigInteger rand = RSAUtil.sample(params.getRandom(), modulo);
        BigInteger additiveRShare = execute(rand, modulo);
        BigInteger paddedShare = rand.modInverse(modulo).multiply(x).mod(modulo);
        params.getNetwork().sendToAll(paddedShare);
        Map<Integer, BigInteger> otherPaddings = params.getNetwork().receiveFromAllPeers();
        BigInteger combinedPadding = otherPaddings.values().stream().reduce(paddedShare, (a,b) -> a.multiply(b).mod(modulo));
        return combinedPadding.multiply(additiveRShare).mod(modulo);
    }

    protected Map<Integer, BigInteger> inverse(BigInteger val, BigInteger modulo) {
        Map<Integer, BigInteger> shares = new HashMap<>(params.getAmountOfPeers()+1);
        BigInteger sum = BigInteger.ZERO;
        for (int i : params.getNetwork().peers()) {
            BigInteger sampled = RSAUtil.sample(params.getRandom(), modulo);
            shares.put(i, sampled);
            sum = sum.add(sampled).mod(modulo);
        }
        BigInteger myShare = val.modInverse(modulo).subtract(sum).mod(modulo);
        shares.put(params.getMyId(), myShare);
        return shares;
    }
}
