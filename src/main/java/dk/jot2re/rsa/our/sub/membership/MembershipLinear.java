package dk.jot2re.rsa.our.sub.membership;

import dk.jot2re.network.NetworkException;
import dk.jot2re.rsa.bf.BFParameters;
import dk.jot2re.rsa.our.RSAUtil;

import java.math.BigInteger;
import java.util.List;

public class MembershipLinear implements IMembership {
    private final BFParameters params;

    public MembershipLinear(BFParameters params) {
        this.params = params;
    }

    public BigInteger execute(BigInteger xShare, List<BigInteger> set, BigInteger modulo) throws NetworkException {
        BigInteger temp = BigInteger.ONE;
        for (BigInteger cur: set) {
            temp = params.getMult().mult(temp, RSAUtil.subConst(params, xShare, cur, modulo), modulo);
        }
        BigInteger rShare = RSAUtil.sample(params, modulo);
        temp = params.getMult().mult(temp, rShare, modulo);
        return temp;
    }
}
