package dk.jot2re.rsa.our;

import dk.jot2re.network.NetworkException;
import dk.jot2re.rsa.our.sub.invert.Invert;
import dk.jot2re.rsa.our.sub.membership.IMembership;
import dk.jot2re.rsa.our.sub.membership.MembershipConst;
import dk.jot2re.rsa.our.sub.membership.MembershipLinear;
import dk.jot2re.rsa.our.sub.membership.MembershipLog;
import dk.jot2re.rsa.our.sub.multToAdd.MultToAdd;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class OurProtocol {
    enum MembershipProtocol {
        CONST,
        LOG,
        LINEAR
    }
    private final OurParameters params;
    private final IMembership membership;
    private final Invert inverter;
    private final MultToAdd multToAdd;
    // List of the set of parties, shifted to ensure positive indexes, plus 1 to account for overflow
    private final List<BigInteger> partySet;

    public OurProtocol(OurParameters params, MembershipProtocol membershipProtocolType) {
        this.params = params;
        this.inverter = new Invert(params);
        this.multToAdd = new MultToAdd(params);
        switch (membershipProtocolType) {
            case CONST -> this.membership = new MembershipConst(params);
            case LOG -> this.membership = new MembershipLog(params);
            case LINEAR -> this.membership = new MembershipLinear(params);
            default -> throw new IllegalArgumentException("Unknown membership protocol");
        }
        partySet = new ArrayList<>(params.getAmountOfPeers()+1);
        for (int i = 1; i <= params.getAmountOfPeers()+2; i++) {
            partySet.add(BigInteger.valueOf(i));
        }

    }

    public boolean execute(BigInteger pShare, BigInteger qShare, BigInteger N) throws NetworkException {
        // TODO validate M bound
        if (params.getP().compareTo(N.multiply(BigInteger.valueOf(params.getAmountOfPeers()+1))) <= 0) {
            throw new IllegalArgumentException("P bound is too small");
        }
        if (params.getQ().compareTo(params.getP()) <= 0) {
            throw new IllegalArgumentException("Q bound is too small");
        }
        if (N.compareTo(params.getM()) >= 0) {
            throw new IllegalArgumentException("Input share is too large");
        }
        if (params.getMyId() == 0) {
            if (!pShare.mod(BigInteger.valueOf(4)).equals(BigInteger.valueOf(3)) ||
                    !qShare.mod(BigInteger.valueOf(4)).equals(BigInteger.valueOf(3))) {
                throw new IllegalArgumentException("P or Q share is congruent to 3 mod 4 for pivot party");
            }
        } else {
            if (!pShare.mod(BigInteger.valueOf(4)).equals(BigInteger.ZERO) ||
                    !qShare.mod(BigInteger.valueOf(4)).equals(BigInteger.ZERO)) {
                throw new IllegalArgumentException("P or Q share is not divisible by 4 for non-pivot party");
            }
        }
        if (!verifyInputs(pShare, qShare, N)) {
            return false;
        }
        if (!verifyPrimality(pShare, N)) {
            return false;
        }
        if (!verifyPrimality(qShare, N)) {
            return false;
        }
        return true;
    }

    protected boolean verifyInputs(BigInteger pShare, BigInteger qShare, BigInteger N) throws NetworkException {
        BigInteger NPrimeShare = params.getMult().mult(pShare, qShare, params.getM());
        BigInteger NPrime = RSAUtil.open(params, NPrimeShare, params.getM());
        if (!NPrime.equals(N)) {
            return false;
        }
        return true;
    }

    protected boolean verifyPrimality(BigInteger share, BigInteger N) throws NetworkException {
        BigInteger multGammaShare;
        if (params.getMyId() == 0) {
            BigInteger v = RSAUtil.sample(params, N);
            params.getNetwork().sendToAll(v);
            multGammaShare = v.modPow(share.subtract(BigInteger.valueOf(1)).shiftRight(1), N);
        } else {
            BigInteger v = params.getNetwork().receive(0);
            multGammaShare = v.modPow(share.shiftRight(1), N);
        }
        BigInteger addGammaShare = multToAdd.execute(multGammaShare, N);
        BigInteger inverseShareP = inverter.execute(share, params.getP());
        BigInteger inverseShareQ = inverter.execute(share, params.getQ());
        BigInteger gammaAdd = RSAUtil.addConst(params, addGammaShare, BigInteger.ONE, N);
        BigInteger gammaSub = RSAUtil.subConst(params, addGammaShare, BigInteger.ONE, N);
        if (!validateGamma(gammaSub, inverseShareP, inverseShareQ, N) &&
                !validateGamma(gammaAdd, inverseShareP, inverseShareQ, N)) {
            return false;
        }
        return true;
    }

    protected boolean validateGamma(BigInteger delta, BigInteger inverseShareP, BigInteger inverseShareQ, BigInteger N) throws NetworkException {
        BigInteger aShare = params.getMult().mult(delta, inverseShareP, params.getP());
        BigInteger bShare = params.getMult().mult(delta, inverseShareQ, params.getQ());
        BigInteger zShare = aShare.subtract(bShare).mod(params.getQ());
        BigInteger temp = zShare.multiply(params.getPInverseModQ()).mod(params.getQ());
        BigInteger zAdjusted = RSAUtil.addConst(params, temp, BigInteger.ONE, params.getQ());
        BigInteger yShare = membership.execute(zAdjusted, partySet, params.getQ());
        BigInteger y = RSAUtil.open(params, yShare, params.getQ());
        return y.equals(BigInteger.ZERO);
    }
}

