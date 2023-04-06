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
        BigInteger temp = RSAUtil.subConst(params, xShare, set.get(0), modulo);
        for (int i = 1; i < set.size(); i++) {
            temp = params.getMult().mult(temp, RSAUtil.subConst(params, xShare, set.get(i), modulo), modulo);
        }
        BigInteger rShare = RSAUtil.sample(params, modulo);
        temp = params.getMult().mult(temp, rShare, modulo);
        return temp;
    }
}
