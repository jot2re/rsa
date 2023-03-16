package dk.jot2re.rsa.our.sub.membership;

import dk.jot2re.network.NetworkException;
import dk.jot2re.rsa.bf.BFParameters;

import java.math.BigInteger;
import java.util.List;

public class MembershipConst {
    private final BFParameters params;

    public MembershipConst(BFParameters params) {
        this.params = params;
    }

    public BigInteger execute(BigInteger xShare, List<BigInteger> set, BigInteger modulo) throws NetworkException {
        return null;
    }
}
