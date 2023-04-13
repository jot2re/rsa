package dk.jot2re.mult.shamir;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShamirEngine {
    private final ShamirResourcePool resources;
    public ShamirEngine(ShamirResourcePool resources) {
        this.resources = resources;
    }

    // code inspired by karbi79 https://stackoverflow.com/questions/19327651/java-implementation-of-shamirs-secret-sharing
    public Map<Integer, BigInteger> share(BigInteger input, BigInteger modulo) {
        BigInteger[] coeff = new BigInteger[resources.getThreshold()];
        for (int i = 1; i < resources.getThreshold(); i++) {
            coeff[i] = resources.getRng().nextBigInteger(modulo);
//            System.out.println("a" + (i + 1) + ": " + coeff[i]);
        }

        final Map<Integer, BigInteger> shares = new HashMap<>(resources.getParties());
        for (int i = 0; i < resources.getParties(); i++) {
            BigInteger currentSum = input;
            for (int j = 1; j < resources.getThreshold(); j++) {
                final BigInteger xPower = BigInteger.valueOf(i+1).modPow(BigInteger.valueOf(j), modulo);
                final BigInteger currentTerm = coeff[j - 1].multiply(xPower).mod(modulo);
                currentSum = currentSum.add(currentTerm);
            }
            shares.put(i, currentSum.mod(modulo));
//            System.out.println("Share " + shares[i - 1]);
        }
        return shares;
    }

    public BigInteger combine(final List<BigInteger> shares, final BigInteger modulo) {
        if (shares.size() < resources.getThreshold()+1) {
            throw new IllegalArgumentException("Not enough shares");
        }
        BigInteger currentSum = BigInteger.ZERO;
        for (int i = 0; i < resources.getThreshold()+1; i++) {
            BigInteger numerator = BigInteger.ONE;
            BigInteger denominator = BigInteger.ONE;

            for (int j = 0; j < resources.getThreshold()+1; j++) {
                if (i != j) {
                    numerator = numerator.multiply(BigInteger.valueOf(-j - 1)).mod(modulo);
                    denominator = denominator.multiply(BigInteger.valueOf(i - j)).mod(modulo);
                }
            }
//            System.out.println("denominator: " + denominator + ", numerator: " + denominator + ", inv: " + denominator.modInverse(primeNum));
            // TODO can just be preprocessed for the specific amount of servers
            final BigInteger lagrange = numerator.multiply(denominator.modInverse(modulo)).mod(modulo);
            final BigInteger tmp = shares.get(i).multiply(lagrange).mod(modulo);
            currentSum = currentSum.add(modulo).add(tmp).mod(modulo);
//            System.out.println("value: " + currentSum + ", tmp: " + tmp + ", currentSum: " + currentSum);
        }
//        System.out.println("The secret is: " + currentSum);
        return currentSum;
    }
}
