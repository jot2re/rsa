package anonymous.mult.shamir;

import anonymous.rsa.our.RSAUtil;

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
    private BigInteger N = BigInteger.ZERO;
    private final BigInteger[] threeBigVals = new BigInteger[] {BigInteger.valueOf(3), BigInteger.valueOf(-3), BigInteger.valueOf(1)};
    private final BigInteger[] fiveBigVals = new BigInteger[]{
            BigInteger.valueOf(5),
            BigInteger.valueOf(-10),
            BigInteger.valueOf(10),
            BigInteger.valueOf(-5),
            BigInteger.valueOf(1),
    };
    private final BigInteger[] sevenBigVals = new BigInteger[]{
            BigInteger.valueOf(7),
            BigInteger.valueOf(-21),
            BigInteger.valueOf(35),
            BigInteger.valueOf(-35),
            BigInteger.valueOf(21),
            BigInteger.valueOf(-7),
            BigInteger.valueOf(1),
    };
    // {{1,1,1,1,1,1,1,1,1},{1,2,4,8,16,32,64,128,256},{1,3,9,27,81, 243,729,2187,6561},{1,4,16,64,256,1024,4096,16384,65536},{1,5,25,125,625,3125,15625,78125,390625},{1,6,36,216,1296,7776,46656,279936,1679616},{1,7,49,343,2401,16807,117649,823543,5764801},{1,8,64,512,4096,32768,262144,2097152,16777216},{1,9,81,729,6561,59049,531441,4782969,43046721}}^-1
    // TODO 9 and 11 are not the correct constant
    private final BigInteger[] nineBigVals = new BigInteger[]{
            BigInteger.valueOf(9),
            BigInteger.valueOf(-36),
            BigInteger.valueOf(45),
            BigInteger.valueOf(-63),
            BigInteger.valueOf(63),
            BigInteger.valueOf(-45),
            BigInteger.valueOf(36),
            BigInteger.valueOf(-9),
            BigInteger.valueOf(1),
    };
    private final BigInteger[] elevenBigVals = new BigInteger[]{
            BigInteger.valueOf(11),
            BigInteger.valueOf(-55),
            BigInteger.valueOf(77),
            BigInteger.valueOf(-99),
            BigInteger.valueOf(121),
            BigInteger.valueOf(-121),
            BigInteger.valueOf(99),
            BigInteger.valueOf(-77),
            BigInteger.valueOf(55),
            BigInteger.valueOf(-11),
            BigInteger.valueOf(1),
    };

    public ShamirEngine(int parties, Random rng) {
        if (parties > 15) {
            throw new RuntimeException("Currently only supports at most 15 parties, due to internal use of long. " +
                    "Needs to be refactored to use BigInteger to work with more parties.");
        }
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
                // Should be used if parties > 15
//                final BigInteger xPower = BigInteger.valueOf(i+1).modPow(BigInteger.valueOf(j), modulo);
                final long xPower = pow(i+1, j);
                final BigInteger currentTerm = coeff[j].multiply(BigInteger.valueOf(xPower)).mod(modulo);
                currentSum = currentSum.add(currentTerm);
            }
            shares.put(i, currentSum.mod(modulo));
        }
        return shares;
    }

    public static long pow(long a, long b){
        long res =1;
        for (int i = 0; i < b; i++) {
            res *= a;
        }
        return res;
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

    protected BigInteger degreeRedConst(int otherId) {
        if (parties == 3) {
            return threeBigVals[otherId];
        }
        if (parties == 5) {
            return fiveBigVals[otherId];
        }
        if (parties == 7) {
            return sevenBigVals[otherId];
        }
        if (parties == 9) {
            return nineBigVals[otherId];
        }
        if (parties == 11) {
            return elevenBigVals[otherId];
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
        return java.math.BigInteger.valueOf(numerator).multiply(java.math.BigInteger.valueOf(denominator).modInverse(modulo)).mod(modulo);
    }
}
