package anonymous.rsa.our;

import anonymous.AbstractProtocol;
import anonymous.compiler.ICompilableProtocol;
import anonymous.mult.ot.util.Fiddling;
import anonymous.network.INetwork;
import anonymous.network.NetworkException;
import anonymous.rsa.our.sub.invert.Invert;
import anonymous.rsa.our.sub.membership.IMembership;
import anonymous.rsa.our.sub.membership.MembershipConst;
import anonymous.rsa.our.sub.membership.MembershipLinear;
import anonymous.rsa.our.sub.membership.MembershipLog;
import anonymous.rsa.our.sub.multToAdd.MultToAdd;
import org.dfdeshom.math.GMP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class OurProtocol extends AbstractProtocol implements ICompilableProtocol {
    private static final Logger logger = LoggerFactory.getLogger(OurProtocol.class);
    public enum MembershipProtocol {
        CONST,
        LOG,
        LINEAR
    }
    private final OurParameters params;
    private final IMembership membership;
    private final Invert inverter;
    private final MultToAdd multToAdd;
    // List of the set of parties, shifted to ensure positive indexes, plus 1 to account for overflow
    private List<BigInteger> partySet;
    private boolean initialized = false;
    private GMP jniN;

    public OurProtocol(OurParameters params, IMembership membership, Invert inverter, MultToAdd multToAdd) {
        this.params = params;
        this.inverter = inverter;
        this.multToAdd = multToAdd;
        this.membership = membership;
    }

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
    }

    @Override
    public void init(INetwork network, Random random) {
        if (!initialized) {
            super.init(network, random);
            membership.init(network, random);
            inverter.init(network, random);
            multToAdd.init(network, random);
            partySet = new ArrayList<>(network.getNoOfParties());
            for (int i = 1; i <= network.getNoOfParties(); i++) {
                partySet.add(BigInteger.valueOf(i));
            }
            initialized = true;
        }
    }


    public boolean validateParameters(BigInteger pShare, BigInteger qShare, BigInteger N) throws NetworkException {
        // TODO validate M bound
        if (params.getP().compareTo(N.multiply(BigInteger.valueOf(network.getNoOfParties()))) <= 0) {
            logger.error("P bound is too small");
            return false;
        }
        if (params.getQ().compareTo(params.getP()) <= 0) {
            logger.error("Q bound is too small");
            return false;
        }
        if (N.compareTo(params.getM()) >= 0) {
            logger.error("Input share is too large");
            return false;
        }
        if (network.myId() == 0) {
            if (!pShare.mod(BigInteger.valueOf(4)).equals(BigInteger.valueOf(3)) ||
                    !qShare.mod(BigInteger.valueOf(4)).equals(BigInteger.valueOf(3))) {
                logger.error("P or Q share is congruent to 3 mod 4 for pivot party");
                return false;
            }
        } else {
            if (!pShare.mod(BigInteger.valueOf(4)).equals(BigInteger.ZERO) ||
                    !qShare.mod(BigInteger.valueOf(4)).equals(BigInteger.ZERO)) {
                logger.error("P or Q share is not divisible by 4 for non-pivot party");
                return false;
            }
        }
        if (!verifyInputs(pShare, qShare, N)) {
            logger.error("Product of P and Q is not the canidate N");
            return false;
        }
        return true;
    }

    @Override
    public List<BigInteger> executeList(List<BigInteger> privateInput, List<BigInteger> publicInput) {
        try {
            boolean res = execute(privateInput.get(0), privateInput.get(1), publicInput.get(0));
            ArrayList<BigInteger> arrayRes =  new ArrayList<>(1);
            arrayRes.add(res ? BigInteger.ONE : BigInteger.ZERO);
            return arrayRes;
        } catch (Exception e) {
            throw new RuntimeException("Party " + network.myId() + " with error " +e.getMessage());
        }
    }

    public boolean execute(BigInteger pShare, BigInteger qShare, BigInteger N) throws NetworkException {
        if (!verifyPrimality(pShare, N) | !verifyPrimality(qShare, N)) {
            return false;
        }
        return true;
    }

    protected boolean verifyInputs(BigInteger pShare, BigInteger qShare, BigInteger N) throws NetworkException {
        BigInteger NPrimeShare = params.getMult().mult(pShare, qShare, params.getM(), params.getPrimeBits());
        BigInteger NPrime = RSAUtil.open(network, NPrimeShare, params.getM());
        if (!NPrime.equals(N)) {
            return false;
        }
        return true;
    }

    protected boolean verifyPrimality(BigInteger share, BigInteger N) throws NetworkException {
        if (params.isJni()) {
            logger.debug("Doing JNI");
            jniN = new GMP(N.toString());
        }
        BigInteger multGammaShare = gammaShare(share, N);
        BigInteger addGammaShare = multToAdd.execute(multGammaShare, N);
        Serializable inverseShareP = inverter.execute(
                params.getMult().shareFromAdditive(share, params.getP()), params.getP());
        Serializable inverseShareQ = inverter.execute(
                params.getMult().shareFromAdditive(share, params.getQ()), params.getQ());
        BigInteger gammaAdd = RSAUtil.addConst(network.myId(), addGammaShare, BigInteger.ONE, N);
        BigInteger gammaSub = RSAUtil.subConst(network.myId(), addGammaShare, BigInteger.ONE, N);
        Serializable ySub = divisibility(gammaSub, inverseShareP, inverseShareQ, N);
        Serializable yAdd = divisibility(gammaAdd, inverseShareP, inverseShareQ, N);
        Serializable yShare = params.getMult().multShares(ySub, yAdd, params.getQ());
        BigInteger y = params.getMult().open(yShare, params.getQ());
        return y.equals(BigInteger.ZERO);
    }

    private BigInteger gammaShare(BigInteger share, BigInteger N) throws NetworkException {
        if (network.myId() == 0) {
            BigInteger v = RSAUtil.sample(random, N);
            network.sendToAll(v);
            BigInteger exp = share.subtract(BigInteger.valueOf(1)).shiftRight(1);
            if (params.isJni()) {
                GMP jniV = new GMP(v.toString());
                GMP jniExp = new GMP(exp.toString());
                jniV.modPow(jniExp, jniN, jniV);
                byte[] vBytes = new byte[Fiddling.ceil(jniV.bitLength(), 8)];
                jniV.toByteArray(vBytes);
                return new BigInteger(1, vBytes);
            } else {
                return v.modPow(exp, N);
            }
        } else {
            BigInteger v = network.receive(0);
            BigInteger exp = share.shiftRight(1);
            if (params.isJni()) {
                GMP jniV = new GMP(v.toString());
                GMP jniExp = new GMP(exp.toString());
                jniV.modPow(jniExp, jniN, jniV);
                byte[] vBytes = new byte[Fiddling.ceil(jniV.bitLength(), 8)];
                jniV.toByteArray(vBytes);
                return new BigInteger(1, vBytes);
            } else {
                return v.modPow(share.shiftRight(1), N);
            }
        }
    }

    protected Serializable divisibility(BigInteger delta, Serializable inverseShareP, Serializable inverseShareQ, BigInteger N) throws NetworkException {
        Serializable deltaP = params.getMult().shareFromAdditive(delta, params.getP());
        Serializable aShare = params.getMult().multShares(deltaP, inverseShareP, params.getP());
        Serializable deltaQ = params.getMult().shareFromAdditive(delta, params.getQ());
        Serializable bShare = params.getMult().multShares(deltaQ, inverseShareQ, params.getQ());
        Serializable zShare = params.getMult().sub(aShare, bShare, params.getQ());
        Serializable temp = params.getMult().multConst(zShare, params.getPInverseModQ(), params.getQ());
//        Serializable zAdjusted = params.getMult().addConst(temp, BigInteger.ONE, params.getQ());
        return membership.execute(temp, partySet, params.getQ());
    }

    public OurParameters getParams() {
        return params;
    }
}

