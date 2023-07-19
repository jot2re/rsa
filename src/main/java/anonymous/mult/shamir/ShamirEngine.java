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
    private BigInteger[] fiveBigVals = new BigInteger[5];
    private BigInteger[] sevenBigVals = new BigInteger[7];
    private BigInteger[] nineBigVals = new BigInteger[9];

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

    protected void setConstants(BigInteger modulo) {
        if (N == modulo) {
            return;
        }
        N = modulo;
        if (parties == 5) {
            BigInteger fiveValsDen = BigInteger.valueOf(12).modInverse(N);
            fiveBigVals = new BigInteger[]{
                    BigInteger.valueOf(-17).multiply(fiveValsDen).mod(N),
                    BigInteger.valueOf(94).multiply(fiveValsDen).mod(N),
                    BigInteger.valueOf(-114).multiply(fiveValsDen).mod(N),
                    BigInteger.valueOf(62).multiply(fiveValsDen).mod(N),
                    BigInteger.valueOf(-13).multiply(fiveValsDen).mod(N),
            };
//           final int[][] fiveValsNum = new int[][] {
//                new int[] {-17, 94, -114, 62, -13},
//                new int[] {-94, 308, -348, 184, -38},
//                new int[] {-171, 522, -582, 306, -63},
//                new int[] {-248, 736, -816, 428, -88},
//                new int[] {-325, 950, -1050, 550, -113},
//        };
        }
        // TODO 7 and 9 are dummy data
        if (parties == 7) {
            BigInteger fiveValsDen = BigInteger.valueOf(25).modInverse(N);
            sevenBigVals = new BigInteger[]{
                    BigInteger.valueOf(-1754).multiply(fiveValsDen).mod(N),
                    BigInteger.valueOf(94514).multiply(fiveValsDen).mod(N),
                    BigInteger.valueOf(-1144).multiply(fiveValsDen).mod(N),
                    BigInteger.valueOf(62584).multiply(fiveValsDen).mod(N),
                    BigInteger.valueOf(-1354).multiply(fiveValsDen).mod(N),
                    BigInteger.valueOf(6884).multiply(fiveValsDen).mod(N),
                    BigInteger.valueOf(-15354).multiply(fiveValsDen).mod(N),
            };
        }
        if (parties == 9) {
            BigInteger fiveValsDen = BigInteger.valueOf(15641).modInverse(N);
            nineBigVals = new BigInteger[]{
                    BigInteger.valueOf(-175324).multiply(fiveValsDen).mod(N),
                    BigInteger.valueOf(94543214).multiply(fiveValsDen).mod(N),
                    BigInteger.valueOf(-1132444).multiply(fiveValsDen).mod(N),
                    BigInteger.valueOf(4242324).multiply(fiveValsDen).mod(N),
                    BigInteger.valueOf(-464364).multiply(fiveValsDen).mod(N),
                    BigInteger.valueOf(643643).multiply(fiveValsDen).mod(N),
                    BigInteger.valueOf(-6436433).multiply(fiveValsDen).mod(N),
                    BigInteger.valueOf(54643643).multiply(fiveValsDen).mod(N),
                    BigInteger.valueOf(-5678645).multiply(fiveValsDen).mod(N),
            };
        }
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

    protected BigInteger degreeRedConst(int myId, int otherId, BigInteger modulo) {
        setConstants(modulo);
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
