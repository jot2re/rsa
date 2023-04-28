package dk.jot2re.rsa.our.sub.membership;

import dk.jot2re.AbstractProtocol;
import dk.jot2re.network.INetwork;
import dk.jot2re.network.NetworkException;
import dk.jot2re.rsa.bf.BFParameters;
import dk.jot2re.rsa.our.RSAUtil;
import dk.jot2re.rsa.our.sub.invert.Invert;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.*;

public class MembershipConst extends AbstractProtocol implements IMembership {
    private final BFParameters params;
    private final Invert inverter;
    private boolean initialized = false;

    public MembershipConst(BFParameters params) {
        this.params = params;
        this.inverter = new Invert(params);
    }

    @Override
    public void init(INetwork network, Random random) {
        if (!initialized) {
            super.init(network, random);
            inverter.init(network, random);
            initialized = true;
        }
    }

    public Serializable execute(Serializable xShare, List<BigInteger> set, BigInteger modulo) throws NetworkException {
        int m = set.size();
        if (m == 0) {
            throw new RuntimeException("empty set");
        }
        if (m == 1) {
            return params.getMult().addConst(xShare, set.get(0).negate(), modulo);
        }
        if (m == 2) {
            Serializable left = params.getMult().addConst(xShare, set.get(0).negate(), modulo);
            Serializable right = params.getMult().addConst(xShare, set.get(1).negate(), modulo);
            return params.getMult().multShares(left, right, modulo);
        }
        // Step 1; sample
        Map<Integer, Serializable> rShares = new HashMap<>(m-1);
        Map<Integer, Serializable> alphaShares = new HashMap<>(m-1);
        for (int i = 2; i <= m; i++) {
            rShares.put(i, params.getMult().shareFromAdditive(RSAUtil.sample(random, modulo), modulo));
            alphaShares.put(i, params.getMult().shareFromAdditive(RSAUtil.sample(random, modulo), modulo));
        }
        // Step 2; invert
        Map<Integer, Serializable> invertedRShares = new HashMap<>(m);
        for (int i: rShares.keySet()) {
            invertedRShares.put(i, inverter.execute(rShares.get(i), modulo));
        }
        Map<Integer, Serializable> invertedAlphaShares = new HashMap<>(m-1);
        for (int i: alphaShares.keySet()) {
            invertedAlphaShares.put(i, inverter.execute(alphaShares.get(i), modulo));
        }
        // Step 3; compute products
        Map<Integer, Serializable> tShares = new HashMap<>(m);
        tShares.put(1, xShare);
        for (int i = 2; i <= m; i++) {
            tShares.put(i, params.getMult().multShares(xShare, invertedRShares.get(i), modulo));
        }
        Map<Integer, Serializable> vShares = new HashMap<>(m-1);
        for (int i: alphaShares.keySet()) {
            vShares.put(i, params.getMult().multShares(alphaShares.get(i), tShares.get(i), modulo));
        }
        Map<Integer, Serializable> wShares = new HashMap<>(m-1);
        for (int i = 2; i <= m; i++) {
            wShares.put(i, params.getMult().multShares(tShares.get(i-1), rShares.get(i), modulo));
        }
        // ... and open these
        Map<Integer, BigInteger> vValues = new HashMap<>(m-1);
        for (int i: vShares.keySet()) {
            vValues.put(i, params.getMult().open(vShares.get(i), modulo));
        }
        Map<Integer, BigInteger> wValues = new HashMap<>(m-1);
        for (int i: wShares.keySet()) {
            wValues.put(i, params.getMult().open(wShares.get(i), modulo));
        }
        // Step 4; compute y values
        Map<Integer, BigInteger> yValues = new HashMap<>(m-1);
        for (int i: vValues.keySet()) {
            BigInteger currentY = vValues.get(i);
            for (int j = 2; j <= i; j++) {
                currentY = currentY.multiply(wValues.get(j)).mod(modulo);
            }
            yValues.put(i, currentY);
        }
        // Step 5; compute coefficients
        BigInteger[] coef = computePolyConsts(set, modulo);
        // Step 6; compute result
        //      x term
        Serializable zShare = params.getMult().multConst(xShare, coef[1], modulo);
        if (network.myId() == 0) {
            //  constant term
            zShare = params.getMult().addConst(zShare, coef[0], modulo);
        }
        //      x^2 to x^(m-1) terms
        for (int i = 2; i < coef.length; i++) {
            zShare = params.getMult().add(zShare,
                    params.getMult().multConst(invertedAlphaShares.get(i),
                            yValues.get(i).multiply(coef[i]).mod(modulo), modulo), modulo);
        }
        //      x^m term
        zShare = params.getMult().add(zShare,
                params.getMult().multConst(invertedAlphaShares.get(m), yValues.get(m), modulo), modulo);
        return zShare;
    }

    protected BigInteger[] computePolyConsts(List<BigInteger> roots, BigInteger modulo) {
        // Each coefficient is defined to be the previous coefficient of same degree, multiplied with current root,
        // adding with the coefficient in the previous iteration that is one degree smaller.
        // coef[i] = root[i]*prevCoef[i]+prevCoef[i-1]
        BigInteger[] lastCoef = new BigInteger[roots.size()];
        lastCoef[0] = roots.get(0).negate().mod(modulo);
        for (int i = 1; i < roots.size(); i++) {
            BigInteger curRoot = roots.get(i).negate().mod(modulo);
            BigInteger[] curCoef = new BigInteger[roots.size()];
            for (int j = 0; j <= i; j++) {
                BigInteger sum = BigInteger.ZERO;
                if (j >= 1) {
                    sum = lastCoef[j-1];
                }
                BigInteger prod = BigInteger.ONE;
                if (j < i) {
                    prod = lastCoef[j];
                }
                curCoef[j] = curRoot.multiply(prod).add(sum).mod(modulo);
            }
            lastCoef = curCoef;
        }
        return lastCoef;
    }
}
