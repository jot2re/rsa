package dk.jot2re.mult.shamir;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

public class ShamirEngine {
    private final ShamirResourcePool resources;
    public ShamirEngine(ShamirResourcePool resources) {
        this.resources = resources;
    }

    // code inspired by karbi79 https://stackoverflow.com/questions/19327651/java-implementation-of-shamirs-secret-sharing

    /**
     * Shares a secret by constructing a polynomial sharing of degree floor (parties/2)
     * @param input
     * @param modulo
     * @return
     */
    public Map<Integer, BigInteger> share(BigInteger input, BigInteger modulo) {
        return randomPoly(resources.getThreshold(), input, modulo);
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
            coeff[i] = resources.getRng().nextBigInteger(modulo);
//            System.out.println("a" + (i + 1) + ": " + coeff[i]);
        }

        final Map<Integer, BigInteger> shares = new HashMap<>(resources.getParties());
        for (int i = 0; i < resources.getParties(); i++) {
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
        if (shares.size() < degree+1) {
            throw new IllegalArgumentException("Not enough shares");
        }
        BigInteger[] coef = lagrangeCoef(degree, modulo);
        BigInteger currentSum = BigInteger.ZERO;
        int ctr = 0;
        for (int i = 0; i < degree+1; i++) {
            // TODO can just be preprocessed for the specific amount of servers
            if (ctr > degree+1) {
                break;
            }
            currentSum = currentSum.add(shares.get(i).multiply(coef[i])).mod(modulo);
            ctr++;
        }
        return currentSum;
    }



    protected BigInteger[] lagrangeCoef(int degree, BigInteger modulo) {
        BigInteger[] coefs = new BigInteger[degree+1];
        for (int i = 1; i < degree+2; i++) {
            coefs[i-1] = lagrangeConst(i, degree, modulo);
        }
        return coefs;
    }

    /**
     * Compute the Lagrange coefficients needed to restore constant term
     * @param xCoord
     * @return
     */
    protected BigInteger lagrangeConst(int xCoord, int degree, BigInteger modulo) {
        BigInteger numerator = BigInteger.ONE;
        BigInteger denominator = BigInteger.ONE;
        for (int i = 1; i < degree+2; i++) {
            if (i != xCoord) {
                numerator = numerator.multiply(BigInteger.valueOf(i));
                denominator = denominator.multiply(BigInteger.valueOf(i - xCoord));
            }
        }
        return numerator.multiply(denominator.modInverse(modulo)).mod(modulo);
    }
}
