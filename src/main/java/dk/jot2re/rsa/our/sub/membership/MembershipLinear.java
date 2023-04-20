package dk.jot2re.rsa.our.sub.membership;

import dk.jot2re.network.NetworkException;
import dk.jot2re.rsa.bf.BFParameters;
import dk.jot2re.rsa.our.RSAUtil;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.List;

public class MembershipLinear implements IMembership {
    private final BFParameters params;

    public MembershipLinear(BFParameters params) {
        this.params = params;
    }

    public Serializable execute(Serializable xShare, List<BigInteger> set, BigInteger modulo) throws NetworkException {
        Serializable temp = params.getMult().addConst(xShare, set.get(0).negate(), modulo);
        for (int i = 1; i < set.size(); i++) {
            temp = params.getMult().multShares(temp, params.getMult().addConst(xShare, set.get(i).negate(), modulo), modulo);
        }
        BigInteger rPart = RSAUtil.sample(params.getRandom(), modulo);
        Serializable rShare = params.getMult().shareFromAdditive(rPart, modulo);
        return params.getMult().multShares(temp, rShare, modulo);
    }
}
