package dk.jot2re.rsa.our;

import dk.jot2re.network.NetworkException;
import dk.jot2re.rsa.bf.BFParameters;

import java.math.BigInteger;

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
            return executePivot(pShare, qShare, N);
        } else {
            if (!pShare.mod(BigInteger.valueOf(4)).equals(BigInteger.ZERO) ||
                    !qShare.mod(BigInteger.valueOf(4)).equals(BigInteger.ZERO)) {
                throw new IllegalArgumentException("P or Q share is not divisible by 4 for non-pivot party");
            }
            return executeOther(pShare, qShare, N);
        }
    }

    protected boolean executePivot(BigInteger pShare, BigInteger qShare, BigInteger N) throws NetworkException {
        return false;
    }

    protected boolean executeOther(BigInteger pShare, BigInteger qShare, BigInteger N) throws NetworkException {
        return false;
    }
}

