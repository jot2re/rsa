package dk.jot2re.rsa.our.sub.multToAdd;

import dk.jot2re.network.NetworkException;
import dk.jot2re.rsa.bf.BFParameters;
import dk.jot2re.rsa.our.RSAUtil;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

public class MultToAdd {
    private final BFParameters params;

    public MultToAdd(BFParameters params) {
        this.params = params;
    }

    public BigInteger execute(BigInteger x, BigInteger modulo) throws NetworkException {
        RandomShare randomShare =  correlatedRandomness(modulo);
        BigInteger paddedShare = executePhase3(randomShare.getMultiplicative(), x, modulo);
        params.getNetwork().sendToAll(paddedShare);
        Map<Integer, BigInteger> otherPaddings = params.getNetwork().receiveFromAllPeers();
        return executePhase4(paddedShare, otherPaddings, randomShare.getAdditive(), modulo);
    }

    public RandomShare correlatedRandomness(BigInteger modulo) throws NetworkException {
        if (params.getMyId() == 0) {
            BigInteger myAdditiveRShare = executePhase1Pivot(modulo);
            Map<Integer, BigInteger> qShares = params.getNetwork().receiveFromAllPeers();
            BigInteger myQShare = executePhase2Pivot(qShares, myAdditiveRShare, modulo);
            Map<Integer, BigInteger> otherMultRShares = params.getNetwork().receiveFromAllPeers();
            BigInteger myMultRShare = otherMultRShares.values().stream().reduce(myQShare, (a, b) -> a.add(b).mod(modulo));
            return new RandomShare(myAdditiveRShare, myMultRShare);
        } else {
            Phase1OtherRes res = executePhase1Other(modulo);
            Map<Integer, BigInteger> othersQShares = new HashMap<>(params.getAmountOfPeers());
            for (int party : params.getNetwork().peers()) {
                params.getNetwork().send(party, res.myQShares.get(party));
                if (party != 0) {
                    othersQShares.put(party, params.getNetwork().receive(party));
                }
            }
            BigInteger pivotMultRShare = executePhase2Other(othersQShares, res.myQShares.get(params.getMyId()), res.myAdditiveRShare, modulo);
            params.getNetwork().send(0, pivotMultRShare);
            return new RandomShare(res.myAdditiveRShare, res.myMultRShare);
        }
    }

    protected BigInteger executePhase1Pivot(BigInteger modulo) {
        return RSAUtil.sample(params, modulo);
    }

    protected Phase1OtherRes executePhase1Other(BigInteger modulo) {
        BigInteger myAdditiveRShare = RSAUtil.sample(params, modulo);
        BigInteger myMultRShare = RSAUtil.sample(params, modulo);
        Map<Integer, BigInteger> myQShares = inverse(myMultRShare, modulo);
        return new Phase1OtherRes(myAdditiveRShare, myMultRShare, myQShares);
    }

    protected BigInteger executePhase2Pivot(Map<Integer, BigInteger> qShares, BigInteger myAdditiveRShare, BigInteger modulo) {
        BigInteger[] toMultiply = new BigInteger[params.getAmountOfPeers()+1];
        qShares.keySet().stream().forEach(i -> toMultiply[i] = qShares.get(i));
        toMultiply[0] = myAdditiveRShare;
        return RSAUtil.multList(params, toMultiply, modulo);
    }

    protected BigInteger executePhase2Other(Map<Integer, BigInteger> otherQShares, BigInteger myQShare, BigInteger myAdditiveRShare, BigInteger modulo) {
        // todo side-effect since the qShares map is now modified
        otherQShares.put(params.getMyId(), myQShare);
        BigInteger[] toMultiply = new BigInteger[params.getAmountOfPeers()+1];
        for (int party : otherQShares.keySet()) {
            toMultiply[party] = otherQShares.get(party);
        }
        toMultiply[0] = myAdditiveRShare;
        return RSAUtil.multList(params, toMultiply, modulo);
    }

    protected BigInteger executePhase3(BigInteger myRMultShare, BigInteger x, BigInteger modulo) {
        return myRMultShare.modInverse(modulo).multiply(x).mod(modulo);
    }

    protected BigInteger executePhase4(BigInteger paddedShare, Map<Integer, BigInteger> otherPaddings, BigInteger myRAdditiveShare, BigInteger modulo) {

        BigInteger combinedPadding = otherPaddings.values().stream().reduce(paddedShare, (a,b) -> a.multiply(b).mod(modulo));
        return combinedPadding.multiply(myRAdditiveShare).mod(modulo);
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

    private class Phase1OtherRes {
        private final BigInteger myAdditiveRShare;
        private final BigInteger myMultRShare;
        private final Map<Integer, BigInteger> myQShares;

        public Phase1OtherRes(BigInteger myAdditiveRShare, BigInteger myMultRShare, Map<Integer, BigInteger> myQShares) {
            this.myAdditiveRShare = myAdditiveRShare;
            this.myMultRShare = myMultRShare;
            this.myQShares = myQShares;
        }
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
