package dk.jot2re.rsa.our.sub.membership;

import dk.jot2re.network.NetworkException;
import dk.jot2re.rsa.bf.BFParameters;
import dk.jot2re.rsa.our.RSAUtil;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class MembershipLog implements IMembership {
    private final BFParameters params;

    public MembershipLog(BFParameters params) {
        this.params = params;
    }

    public Serializable execute(Serializable xShare, List<BigInteger> set, BigInteger modulo) throws NetworkException {
        List<Serializable> toMult = new ArrayList<>(set.size());
        for (int i = 0; i < set.size(); i++) {
            toMult.add(params.getMult().addConst(xShare, set.get(i).negate(), modulo));
        }
        while (toMult.size() > 1) {
            List<Serializable> nextToMult = new ArrayList<>(1+toMult.size()/2);
            for (int i = 0; i < toMult.size(); i=i+2) {
                if (i+1 < toMult.size()) {
                    nextToMult.add(params.getMult().multShares(toMult.get(i), toMult.get(i + 1), modulo));
                } else {
                    nextToMult.add(toMult.get(i));
                }
            }
            toMult = nextToMult;
        }
        BigInteger rPart = RSAUtil.sample(params.getRandom(), modulo);
        Serializable rShare = params.getMult().shareFromAdditive(rPart, modulo);
        return params.getMult().multShares(toMult.get(0), rShare, modulo);
    }
}
