package dk.jot2re.rsa.bf;

import dk.jot2re.network.NetworkException;
import dk.jot2re.rsa.bf.dto.Phase1Other;
import dk.jot2re.rsa.bf.dto.Phase1Pivot;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

public class Protocol {
    private final BFParameters params;

    public Protocol(BFParameters params) {
        this.params = params;
    }

    public boolean execute(BigInteger pShare, BigInteger qShare, BigInteger N) throws NetworkException {
            if (params.getMyId() == 0) {
                if (!pShare.mod(BigInteger.valueOf(4)).equals(BigInteger.valueOf(3)) ||
                        !qShare.mod(BigInteger.valueOf(4)).equals(BigInteger.valueOf(3))) {
                    throw new IllegalArgumentException("P or Q share is congruent to 3 mod 4 for pivot party");
                }
                Phase1Pivot dto = executePhase1Pivot(pShare, qShare, N);
                params.getNetwork().sendToAll(dto);
                Map<Integer, Phase1Other> receivedDto = params.getNetwork().receiveFromAllPeers();
                return executePhase2(receivedDto, dto.getNuShares(), N);
            } else {
                if (!pShare.mod(BigInteger.valueOf(4)).equals(BigInteger.ZERO) ||
                        !qShare.mod(BigInteger.valueOf(4)).equals(BigInteger.ZERO)) {
                    throw new IllegalArgumentException("P or Q share is not divisible by 4 for non-pivot party");
                }
                Phase1Pivot receivedDto = (Phase1Pivot) params.getNetwork().receive(0);
                Phase1Other dto = executePhase1Other(receivedDto.getGammas(), pShare, qShare, N);
                params.getNetwork().sendToAll(dto);
                Map<Integer, Phase1Other> nuShares = params.getNetwork().receiveFromNonPivotPeers();
                nuShares.put(0, new Phase1Other(receivedDto.getNuShares()));
                return executePhase2(nuShares, dto.getNuShares(),  N);
            }
    }

    protected Phase1Pivot executePhase1Pivot(BigInteger pShare, BigInteger qShare, BigInteger N) {
        Phase1Pivot dto = new Phase1Pivot();
        for (int i = 0; i < params.getStatBits(); i++) {
            BigInteger gamma = sampleGamma(N);
            BigInteger exponentNumerator = N.add(BigInteger.ONE).subtract(pShare).subtract(qShare);
            BigInteger exponentDenominator = BigInteger.valueOf(4);
            BigInteger exponent = exponentNumerator.divide(exponentDenominator);
            BigInteger nuShare = gamma.modPow(exponent, N);
            dto.addElements(gamma, nuShare);
        }
        return dto;
    }

    protected Phase1Other executePhase1Other(List<BigInteger> gammas, BigInteger pShare, BigInteger qShare, BigInteger N) {
        Phase1Other dto = new Phase1Other();
        for (int i = 0; i < params.getStatBits(); i++) {
            BigInteger exponentNumerator = pShare.negate().subtract(qShare);
            BigInteger exponentDenominator = BigInteger.valueOf(4);
            BigInteger exponent = exponentNumerator.divide(exponentDenominator);
            BigInteger nuShare = gammas.get(i).modPow(exponent, N);
            dto.addElements(nuShare);
        }
        return dto;
    }

    protected boolean executePhase2(Map<Integer, Phase1Other> nuShares, List<BigInteger> myNuShares, BigInteger N) {
        for (int i = 0; i < params.getStatBits(); i++) {
            BigInteger nu = myNuShares.get(i);
            for (int j : params.getNetwork().peers()) {
                nu = nuShares.get(j).getNuShares().get(i).multiply(nu).mod(N);
            }
            if (!nu.equals(BigInteger.ONE) && !nu.equals(N.subtract(BigInteger.ONE))) {
                return false;
            }
        }
        return true;
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