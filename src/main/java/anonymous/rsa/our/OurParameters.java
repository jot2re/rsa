package anonymous.rsa.our;

import anonymous.mult.IMult;
import anonymous.rsa.bf.BFParameters;

import java.math.BigInteger;

public class OurParameters extends BFParameters {
    private final BigInteger P;
    private final BigInteger Q;
    private final BigInteger M;
    private final BigInteger PInverseModQ;

    public OurParameters(int primeBits, int statBits, BigInteger P, BigInteger Q, BigInteger M, IMult mult) {
        super(primeBits, statBits, mult);
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
