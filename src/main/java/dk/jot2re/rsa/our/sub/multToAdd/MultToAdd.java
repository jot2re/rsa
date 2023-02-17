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

    public BigInteger execute(BigInteger x, BigInteger modulo) throws NetworkException {
        if (params.getMyId() == 0) {
            BigInteger myAdditiveRShare = executePhase1Pivot(modulo);
            Map<Integer, BigInteger> qShares = params.getNetwork().receiveFromAllPeers();
            BigInteger myMultRShare = executePhase2Pivot(qShares, myAdditiveRShare);
            Map<Integer, BigInteger> otherMultRShares = params.getNetwork().receiveFromAllPeers();
            return executePhase3Pivot(otherMultRShares, myMultRShare, myAdditiveRShare, x, modulo);
        } else {
            Phase1OtherRes res = executePhase1Other(modulo);
            Map<Integer, BigInteger> othersQShares = new HashMap<>(params.getParties());
            for (int party : params.getNetwork().peers()) {
                params.getNetwork().send(party, res.myQShares.get(party));
                othersQShares.put(party, params.getNetwork().receive(party));
            }
            BigInteger myMultRShare = executePhase2Other(othersQShares, res.myQShares.get(params.getMyId()), res.myAdditiveRShare);
            params.getNetwork().send(0, myMultRShare);
            return executePhase3Other(myMultRShare, res.myAdditiveRShare, x, modulo);
        }
    }

    protected BigInteger executePhase1Pivot(BigInteger modulo) {
        return RSAUtil.sample(params, modulo);
    }

    protected Phase1OtherRes executePhase1Other(BigInteger modulo) {
        BigInteger myAdditiveRShare = RSAUtil.sample(params, modulo);
        Map<Integer, BigInteger> myQShares = inverse(RSAUtil.sample(params, modulo), modulo);
        return new Phase1OtherRes(myAdditiveRShare, myQShares);
    }

    protected BigInteger executePhase2Pivot(Map<Integer, BigInteger> qShares, BigInteger rShare) {
        List<BigInteger> toMultiply = qShares.values().stream().sorted().toList();
        toMultiply.add(rShare);
        return RSAUtil.multList(params, toMultiply);
    }

    protected BigInteger executePhase2Other(Map<Integer, BigInteger> otherQShares, BigInteger myQShare, BigInteger myRShare) {
        // todo side-effect since the qShares map is now modified
        otherQShares.put(params.getMyId(), myQShare);
        List<BigInteger> toMultiply = otherQShares.values().stream().sorted().toList();
        toMultiply.add(myRShare);
        return RSAUtil.multList(params, toMultiply);
    }

    protected BigInteger executePhase3Pivot(Map<Integer, BigInteger> otherRMultShares, BigInteger myRMult, BigInteger myRAdditiveShare, BigInteger x, BigInteger modulo) {
        BigInteger rMult = otherRMultShares.values().stream().reduce(myRMult, (a, b) -> a.add(b).mod(modulo));
        BigInteger paddedShare = rMult.modInverse(modulo).multiply(x).mod(modulo);
        return paddedShare.multiply(myRAdditiveShare).mod(modulo);
    }

    protected BigInteger executePhase3Other(BigInteger rMult, BigInteger myRAdditiveShare, BigInteger x, BigInteger modulo) {
        BigInteger paddedShare = rMult.modInverse(modulo).multiply(x).mod(modulo);
        return paddedShare.multiply(myRAdditiveShare).mod(modulo);
    }

    protected Map<Integer, BigInteger> inverse(BigInteger val, BigInteger modulo) {
        Map<Integer, BigInteger> shares = new HashMap<>(params.getParties());
        BigInteger sum = BigInteger.ZERO;
        for (int i : params.getNetwork().peers()) {
            BigInteger sampled = RSAUtil.sample(params, modulo);
            shares.put(i, sampled);
            sum = sum.add(sampled).mod(modulo);
        }
        BigInteger myShares = val.modInverse(modulo).subtract(sum).mod(modulo);
        shares.put(params.getMyId(), myShares);
        return shares;
    }

    private class Phase1OtherRes {
        private final BigInteger myAdditiveRShare;
        private final Map<Integer, BigInteger> myQShares;

        public Phase1OtherRes(BigInteger myAdditiveRShare, Map<Integer, BigInteger> myQShares) {
            this.myAdditiveRShare = myAdditiveRShare;
            this.myQShares = myQShares;
        }
    }
}
