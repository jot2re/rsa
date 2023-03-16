package dk.jot2re.rsa.our.sub.membership;

import dk.jot2re.network.NetworkException;
import dk.jot2re.rsa.bf.BFParameters;
import dk.jot2re.rsa.our.RSAUtil;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class MembershipLog {
    private final BFParameters params;

    public MembershipLog(BFParameters params) {
        this.params = params;
    }

    public BigInteger execute(BigInteger xShare, List<BigInteger> set, BigInteger modulo) throws NetworkException {
        BigInteger temp = BigInteger.ONE;
        List<BigInteger> toMult = new ArrayList<>(set.size());
        for (int i = 0; i < set.size(); i++) {
            if (params.getMyId() == 0) {
                toMult.add(params.getMult().mult(temp, xShare.subtract(set.get(i)).mod(modulo), modulo));
            } else {
                toMult.add(params.getMult().mult(temp, xShare, modulo));
            }
        }
        while (toMult.size() > 1) {
            List<BigInteger> nextToMult = new ArrayList<>(1+toMult.size()/2);
            for (int i = 0; i < toMult.size(); i=i+2) {
                if (i+1 < toMult.size()) {
                    nextToMult.add(params.getMult().mult(toMult.get(i), toMult.get(i + 1), modulo));
                } else {
                    nextToMult.add(toMult.get(i));
                }
            }
            toMult = nextToMult;
        }
        BigInteger rShare = RSAUtil.sample(params, modulo);
        return params.getMult().mult(toMult.get(0), rShare, modulo);
    }
}