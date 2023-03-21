package dk.jot2re.rsa.our;

import dk.jot2re.mult.IMult;
import dk.jot2re.network.INetwork;
import dk.jot2re.rsa.bf.BFParameters;

import java.math.BigInteger;
import java.security.SecureRandom;

public class OurParameters extends BFParameters {
    private final BigInteger P;
    private final BigInteger Q;
    private final BigInteger M;
    private final BigInteger PInverseModQ;

    public OurParameters(int primeBits, int statBits, BigInteger P, BigInteger Q, BigInteger M, INetwork network, IMult mult, SecureRandom rand) {
        super(primeBits, statBits, network, mult, rand);
        this.P = P;
        this.Q = Q;
        this.M = M;
        this.PInverseModQ = P.modInverse(Q);
    }

    public BigInteger getP() {
        return P;
    }

    public BigInteger getQ() {
        return Q;
    }

    public BigInteger getM() {
        return M;
    }

    public BigInteger getPInverseModQ() {
        return PInverseModQ;
    }
}
