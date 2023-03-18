package dk.jot2re.rsa.our.sub.membership;

import dk.jot2re.network.NetworkException;
import dk.jot2re.rsa.bf.BFParameters;
import dk.jot2re.rsa.our.RSAUtil;
import dk.jot2re.rsa.our.sub.invert.Invert;

import java.math.BigInteger;
import java.util.*;

public class MembershipConst implements IMembership {
    private final BFParameters params;

    public MembershipConst(BFParameters params) {
        this.params = params;
    }

    public BigInteger execute(BigInteger xShare, List<BigInteger> set, BigInteger modulo) throws NetworkException {
        // TODO handle edgecase when set is smaller than 2
        int m = set.size();
        // Step 1; sample
        BigInteger rho = RSAUtil.sample(params, modulo);
        Map<Integer, BigInteger> rShares = new HashMap<>(m);
        rShares.put(1, RSAUtil.sample(params, modulo));
        Map<Integer, BigInteger> alphaShares = new HashMap<>(m-1);
        for (int i = 2; i <= m; i++) {
            rShares.put(i, RSAUtil.sample(params, modulo));
            alphaShares.put(i, RSAUtil.sample(params, modulo));
        }
        // Step 2; invert
        Invert inverter = new Invert(params);
        Map<Integer, BigInteger> invertedRShares = new HashMap<>(m);
        for (int i: rShares.keySet()) {
            invertedRShares.put(i, inverter.execute(rShares.get(i), modulo));
        }
        Map<Integer, BigInteger> invertedAlphaShares = new HashMap<>(m-1);
        for (int i: alphaShares.keySet()) {
            invertedAlphaShares.put(i, inverter.execute(alphaShares.get(i), modulo));
        }
        // Step 3; compute products
        Map<Integer, BigInteger> tShares = new HashMap<>(m);
        for (int i: invertedRShares.keySet()) {
            tShares.put(i, params.getMult().mult(xShare, invertedRShares.get(i), modulo));
        }
        Map<Integer, BigInteger> vShares = new HashMap<>(m-1);
        for (int i: alphaShares.keySet()) {
            BigInteger temp = params.getMult().mult(alphaShares.get(i), tShares.get(i), modulo);
            vShares.put(i, params.getMult().mult(temp, rShares.get(1), modulo));
        }
        Map<Integer, BigInteger> wShares = new HashMap<>(m-1);
        for (int i = 2; i <= m; i++) {
            wShares.put(i, params.getMult().mult(tShares.get(i-1), rShares.get(i), modulo));
        }
        // ... and open these
        Map<Integer, BigInteger> vValues = new HashMap<>(m-1);
        for (int i: vShares.keySet()) {
            vValues.put(i, RSAUtil.open(params, vShares.get(i), modulo));
        }
        Map<Integer, BigInteger> wValues = new HashMap<>(m-1);
        for (int i: wShares.keySet()) {
            wValues.put(i, RSAUtil.open(params, wShares.get(i), modulo));
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
        BigInteger zShare = coef[1].multiply(xShare).mod(modulo);
        if (params.getMyId() == 0) {
            //  constant term
            zShare = zShare.add(coef[0]).mod(modulo);
        }
        //      x^2 to x^(m-1) terms
        for (int i = 2; i < coef.length; i++) {
            zShare = zShare.add(yValues.get(i).multiply(coef[i]).multiply(invertedAlphaShares.get(i))).mod(modulo);
        }
        //      x^m term
        zShare = zShare.add(invertedAlphaShares.get(m).multiply(yValues.get(m))).mod(modulo);
        return params.getMult().mult(zShare, rho, modulo);
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
