package dk.jot2re.rsa.bf;

import dk.jot2re.AbstractProtocol;
import dk.jot2re.network.INetwork;
import dk.jot2re.network.NetworkException;
import dk.jot2re.rsa.bf.dto.Phase1Pivot;
import dk.jot2re.rsa.our.RSAUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class BFProtocol extends AbstractProtocol {
    private static final Logger logger = LoggerFactory.getLogger(BFProtocol.class);
    private final BFParameters params;

    public BFProtocol(BFParameters params) {
        this.params = params;
    }

    @Override
    public void init(INetwork network, Random random) {
        super.init(network, random);
        params.getMult().init(network, random);
    }

    public boolean validateParameters(BigInteger pShare, BigInteger qShare, BigInteger N) throws NetworkException {
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
            logger.error("Product of P and Q is not the candidate N");
            return false;
        }
        return true;
    }

    public boolean execute(BigInteger pShare, BigInteger qShare, BigInteger N) throws NetworkException {
        if (network.myId() == 0) {
            return executePivot(pShare, qShare, N);
        } else {
            return executeOther(pShare, qShare, N);
        }
    }

    protected boolean executePivot(BigInteger pShare, BigInteger qShare, BigInteger N) throws NetworkException {
        Phase1Pivot dto = executePhase1Pivot(pShare, qShare, N);
        network.sendToAll(dto.getGammas());
        network.sendToAll(dto.getNuShares());
        Map<Integer, ArrayList<BigInteger>> receivedDto = network.receiveFromAllPeers();
        if (!executePhase2(receivedDto, dto.getNuShares(), N)) {
            return false;
        }
        return executePhase3Pivot(pShare, qShare, N);
    }

    protected boolean executeOther(BigInteger pShare, BigInteger qShare, BigInteger N) throws NetworkException {
        ArrayList<BigInteger> gamma = network.receive(0);
        ArrayList<BigInteger> nu = network.receive(0);
        Phase1Pivot receivedDto = new Phase1Pivot(gamma, nu);
        ArrayList<BigInteger> myNuShare = executePhase1Other(receivedDto.getGammas(), pShare, qShare, N);
        network.sendToAll(myNuShare);
        Map<Integer, ArrayList<BigInteger>> nuShares = network.receiveFromNonPivotPeers();
        nuShares.put(0, receivedDto.getNuShares());
        if (!executePhase2(nuShares, myNuShare,  N)) {
            return false;
        }
        return executePhase3Other(pShare, qShare, N);
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

    protected ArrayList<BigInteger> executePhase1Other(List<BigInteger> gammas, BigInteger pShare, BigInteger qShare, BigInteger N) {
        ArrayList<BigInteger> dto = new ArrayList<>(params.getStatBits());
        for (int i = 0; i < params.getStatBits(); i++) {
            BigInteger exponentNumerator = pShare.negate().subtract(qShare);
            BigInteger exponentDenominator = BigInteger.valueOf(4);
            BigInteger exponent = exponentNumerator.divide(exponentDenominator);
            BigInteger nuShare = gammas.get(i).modPow(exponent, N);
            dto.add(nuShare);
        }
        return dto;
    }

    protected boolean executePhase2(Map<Integer, ArrayList<BigInteger>> nuShares, List<BigInteger> myNuShares, BigInteger N) {
        for (int i = 0; i < params.getStatBits(); i++) {
            BigInteger nu = myNuShares.get(i);
            for (int j : network.peers()) {
                nu = nuShares.get(j).get(i).multiply(nu).mod(N);
            }
            if (!nu.equals(BigInteger.ONE) && !nu.equals(N.subtract(BigInteger.ONE))) {
                return false;
            }
        }
        return true;
    }

    protected boolean executePhase3Pivot(BigInteger p, BigInteger q, BigInteger N) {
        BigInteger r = RSAUtil.sample(random, N);
        Serializable rShare = params.getMult().shareFromAdditive(r, N);
        Serializable toMult = params.getMult().shareFromAdditive(p.add(q).subtract(BigInteger.ONE), N);
        Serializable res = params.getMult().multShares(rShare, toMult, N);
        BigInteger s = params.getMult().open(res, N);
        return s.gcd(N).equals(BigInteger.ONE);
    }

    protected boolean executePhase3Other(BigInteger p, BigInteger q, BigInteger N) {
        BigInteger r = RSAUtil.sample(random, N);
        Serializable rShare = params.getMult().shareFromAdditive(r, N);
        Serializable toMult = params.getMult().shareFromAdditive(p.add(q), N);
        Serializable res = params.getMult().multShares(rShare, toMult, N);
        BigInteger s = params.getMult().open(res, N);
        return s.gcd(N).equals(BigInteger.ONE);
    }

    private BigInteger sampleGamma(BigInteger N) {
        BigInteger candidate;
        do {
            candidate = new BigInteger(N.bitLength() + params.getStatBits(), random);
        } while (jacobiSymbol(candidate, N) != 1);
        return candidate;
    }

    protected boolean verifyInputs(BigInteger pShare, BigInteger qShare, BigInteger N) throws NetworkException {
        BigInteger NPrimeShare = params.getMult().mult(pShare, qShare, N);
        BigInteger NPrime = RSAUtil.open(network, NPrimeShare, N);
        if (!NPrime.equals(BigInteger.ZERO)) {
            return false;
        }
        return true;
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