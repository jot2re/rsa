package dk.jot2re.rsa.bf;

import dk.jot2re.network.NetworkException;

import java.math.BigInteger;
import java.util.List;

public class Protocol {
    private final BFParameters params;

    public Protocol(BFParameters params) {
        this.params = params;
    }

    public boolean execute(BigInteger pShare, BigInteger qShare, BigInteger N) throws NetworkException {
        BigInteger gamma, exponentNumerator;
        if (params.getMyId() == 0) {
            if (!pShare.mod(BigInteger.valueOf(4)).equals(BigInteger.valueOf(3)) ||
                    !qShare.mod(BigInteger.valueOf(4)).equals(BigInteger.valueOf(3))) {
                throw new IllegalArgumentException("P or Q share is congruent to 3 mod 4 for pivot party");
            }
            gamma = sampleGamma(N);
            params.getNetwork().sendToAll(gamma);
            exponentNumerator = N.add(BigInteger.ONE).subtract(pShare).subtract(qShare);
        } else {
            if (!pShare.mod(BigInteger.valueOf(4)).equals(BigInteger.ZERO) ||
                    !qShare.mod(BigInteger.valueOf(4)).equals(BigInteger.ZERO)) {
                throw new IllegalArgumentException("P or Q share is not divisible by 4 for non-pivot party");
            }
            gamma = (BigInteger) params.getNetwork().receive(0);
            exponentNumerator = pShare.negate().subtract(qShare);
        }
        BigInteger exponentDenominator = BigInteger.valueOf(4);
        BigInteger exponent = exponentNumerator.divide(exponentDenominator);
        BigInteger nuShare = gamma.modPow(exponent, N);
        params.getNetwork().sendToAll(nuShare);
        List<BigInteger> nuShares = params.getNetwork().receiveList();
        BigInteger nu = nuShares.stream().reduce(nuShare, (a, b) -> a.multiply(b).mod(N));
        return nu.equals(BigInteger.ONE) || nu.equals(N.subtract(BigInteger.ONE));
    }

    private BigInteger sampleGamma(BigInteger N) {
        BigInteger candidate;
        do {
            candidate = new BigInteger(N.bitLength() + params.getStatBits(), params.getRandom());
        } while (jacobiSymbol(candidate, N) != 1);
        return candidate;
    }

    // Code shamelessly stolen from https://rosettacode.org/wiki/Jacobi_symbol and adapted to work with big integers.
    // Initial code is under GNU license.
    protected static int jacobiSymbol(BigInteger k, BigInteger n) {
        if (k.compareTo(BigInteger.ZERO) < 0 || n.mod(BigInteger.TWO).equals(BigInteger.ZERO)) {
            throw new IllegalArgumentException("Invalid value. k = " + k + ", n = " + n);
        }
        k = k.mod(n);
        int jacobi = 1;
        while (k.compareTo(BigInteger.ZERO) > 0) {
            while (k.mod(BigInteger.TWO).equals(BigInteger.ZERO)) {
                k = k.shiftRight(1);
                BigInteger r = n.mod(BigInteger.valueOf(8));
                if (r.equals(BigInteger.valueOf(3)) || r.equals(BigInteger.valueOf(5))) {
                    jacobi = -jacobi;
                }
            }
            BigInteger temp = n;
            n = k;
            k = temp;
            if (k.mod(BigInteger.valueOf(4)).equals(BigInteger.valueOf(3)) && n.mod(BigInteger.valueOf(4)).equals(BigInteger.valueOf(3))) {
                jacobi = -jacobi;
            }
            k = k.mod(n);
        }
        if (n.equals(BigInteger.ONE)) {
            return jacobi;
        }
        return 0;
    }


}