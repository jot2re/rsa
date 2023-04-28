package dk.jot2re.mult.shamir;

import dk.jot2re.rsa.our.RSAUtil;

import java.math.BigInteger;
import java.util.*;

/**
 * Assumes parties have IDs incrementally starting from 0 to n-1.
 * Points on the Shamir polynomials map party ID -> party ID + 1, i.e. the IDs are 1...n
 * The implementation is furthermore semi-honest and thus assumes all party shares exist when trying to do reconstruction.
 * (or rather assumes the first (n-1)/2 are present).
 */
public class ShamirEngine {
    private final int parties;
    private final int threshold;
    private final Random rng;
    public ShamirEngine(int parties, Random rng) {
        this.parties = parties;
        this.threshold = (parties-1)/2;
        this.rng = rng;
    }

    public int getParties() {
        return parties;
    }
    public int getThreshold() {
        return threshold;
    }

    // code inspired by karbi79 https://stackoverflow.com/questions/19327651/java-implementation-of-shamirs-secret-sharing

    /**
     * Shares a secret by constructing a polynomial sharing of degree floor (parties/2)
     * @param input
     * @param modulo
     * @return
     */
    public Map<Integer, BigInteger> share(BigInteger input, BigInteger modulo) {
        return randomPoly(threshold, input, modulo);
    }

    /**
     * Returns a random polynomial with a specific degree and constant term, working over a modulo.
     * @param degree polynomial degree
     * @param input constant term to share
     * @param modulo modulo
     * @return points on the polynomial, from 0..parties-1
     */
    public Map<Integer, BigInteger> randomPoly(int degree, BigInteger input, BigInteger modulo) {
        BigInteger[] coeff = new BigInteger[degree+1];
        coeff[0] = input;
        for (int i = 1; i < degree+1; i++) {
            coeff[i] = RSAUtil.sample(rng, modulo);
        }

        final Map<Integer, BigInteger> shares = new HashMap<>(parties);
        for (int i = 0; i < parties; i++) {
            BigInteger currentSum = coeff[0];
            for (int j = 1; j < degree+1; j++) {
                final BigInteger xPower = BigInteger.valueOf(i+1).modPow(BigInteger.valueOf(j), modulo);
                final BigInteger currentTerm = coeff[j].multiply(xPower).mod(modulo);
                currentSum = currentSum.add(currentTerm);
            }
            shares.put(i, currentSum.mod(modulo));
        }
        return shares;
    }

    public BigInteger combine(final int degree, final Map<Integer, BigInteger> shares, final BigInteger modulo) {
        List<BigInteger> res = new ArrayList<>(shares.size());
        for (int i = 0; i < shares.size(); i++) {
            res.add(shares.get(i));
        }
        return combine(degree, res, modulo);
    }

    public BigInteger combine(final int degree, final List<BigInteger> shares, final BigInteger modulo) {
        if (shares.size() < degree+1) {
            throw new IllegalArgumentException("Not enough shares");
        }
        BigInteger[] coef = lagrangeCoef(degree, modulo);
        BigInteger currentSum = BigInteger.ZERO;
        for (int i = 0; i < degree+1; i++) {
            // TODO can just be preprocessed for the specific amount of servers
            currentSum = currentSum.add(shares.get(i).multiply(coef[i])).mod(modulo);
        }
        return currentSum;
    }

    protected BigInteger degreeRedConst(int myId, int otherId, BigInteger modulo) {
        final int[] threeVals = new int[] {3, -3, 1};
        final int[][] fiveValsNum = new int[][] {
                new int[] {-17, 94, -114, 62, -13},
                new int[] {-94, 308, -348, 184, -38},
                new int[] {-171, 522, -582, 306, -63},
                new int[] {-248, 736, -816, 428, -88},
                new int[] {-325, 950, -1050, 550, -113},
        };
        BigInteger fiveValsDen = BigInteger.valueOf(12).modInverse(modulo);
        if (parties == 3) {
            return BigInteger.valueOf(threeVals[otherId]);
        }
        if (parties == 5) {
            return BigInteger.valueOf(fiveValsNum[myId][otherId]).multiply(fiveValsDen).mod(modulo);
        }
        throw new RuntimeException("not found yet");
    }

    protected static BigInteger[] lagrangeCoef(int degree, BigInteger modulo) {
        BigInteger[] coefs = new BigInteger[degree+1];
        for (int i = 0; i < degree+1; i++) {
            coefs[i] = lagrangeConst(i+1, degree, modulo);
        }
        return coefs;
    }

    /**
     * Compute the Lagrange coefficients needed to restore constant term
     * @param xCoord
     * @return
     */
    protected static BigInteger lagrangeConst(long xCoord, int degree, BigInteger modulo) {
        long numerator = 1;
        long denominator = 1;
        for (long i = 1; i < degree+2; i++) {
            if (i != xCoord) {
                numerator = numerator * i;
                denominator = denominator * (i - xCoord);
            }
        }
        return BigInteger.valueOf(numerator).multiply(BigInteger.valueOf(denominator).modInverse(modulo)).mod(modulo);
    }
}
