package dk.jot2re.rsa.our.sub.multToAdd;

import dk.jot2re.network.NetworkException;
import dk.jot2re.rsa.bf.BFParameters;
import dk.jot2re.rsa.our.RSAUtil;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
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
        List<BigInteger> myAdditiveShares = RSAUtil.share(params, x, modulo);
        return computeAdditiveShareOfProduct(myAdditiveShares, modulo);
    }

    public BigInteger executeWithCorrelatedRandomness(BigInteger x, BigInteger modulo) throws NetworkException {
        RandomShare randomShare =  correlatedRandomness(modulo);
        BigInteger paddedShare = randomShare.getMultiplicative().modInverse(modulo).multiply(x).mod(modulo);
        params.getNetwork().sendToAll(paddedShare);
        Map<Integer, BigInteger> otherPaddings = params.getNetwork().receiveFromAllPeers();
        BigInteger combinedPadding = otherPaddings.values().stream().reduce(paddedShare, (a,b) -> a.multiply(b).mod(modulo));
        return combinedPadding.multiply(randomShare.additive).mod(modulo);
    }

    public RandomShare correlatedRandomness(BigInteger modulo) {
        List<BigInteger> myAdditiveShares = RSAUtil.randomSharing(params, modulo);
        BigInteger myAdditiveRShare = computeAdditiveShareOfProduct(myAdditiveShares, modulo);
        BigInteger myMultShare = myAdditiveShares.stream().reduce(BigInteger.ZERO, BigInteger::add).mod(modulo);
        return new RandomShare(myAdditiveRShare, myMultShare);
    }

    protected BigInteger computeAdditiveShareOfProduct(List<BigInteger> myAdditiveShares, BigInteger modulo) {
        for (int party : params.getNetwork().peers()) {
            params.getNetwork().send(party, myAdditiveShares.get(party));
        }
        Map<Integer, BigInteger> othersAdditiveShares = params.getNetwork().receiveFromAllPeers();
        othersAdditiveShares.put(params.getMyId(), myAdditiveShares.get(params.getMyId()));
        BigInteger myAdditiveShare = RSAUtil.multList(params, othersAdditiveShares.values().toArray(new BigInteger[0]), modulo);
        return myAdditiveShare;
    }

    protected Map<Integer, BigInteger> inverse(BigInteger val, BigInteger modulo) {
        Map<Integer, BigInteger> shares = new HashMap<>(params.getAmountOfPeers()+1);
        BigInteger sum = BigInteger.ZERO;
        for (int i : params.getNetwork().peers()) {
            BigInteger sampled = RSAUtil.sample(params, modulo);
            shares.put(i, sampled);
            sum = sum.add(sampled).mod(modulo);
        }
        BigInteger myShare = val.modInverse(modulo).subtract(sum).mod(modulo);
        shares.put(params.getMyId(), myShare);
        return shares;
    }

    public class RandomShare {
        private final BigInteger additive;
        private final BigInteger multiplicative;
        public RandomShare(BigInteger additive, BigInteger multiplicative) {
            this.additive = additive;
            this.multiplicative = multiplicative;
        }
        public BigInteger getAdditive() {
            return additive;
        }
        public BigInteger getMultiplicative() {
            return multiplicative;
        }
    }
}
