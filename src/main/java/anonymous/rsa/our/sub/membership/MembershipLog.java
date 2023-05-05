package anonymous.rsa.our.sub.membership;

import anonymous.AbstractProtocol;
import anonymous.network.INetwork;
import anonymous.network.NetworkException;
import anonymous.rsa.bf.BFParameters;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MembershipLog extends AbstractProtocol implements IMembership {
    private final BFParameters params;
    private boolean initialized = false;

    public MembershipLog(BFParameters params) {
        this.params = params;
    }

    @Override
    public void init(INetwork network, Random random) {
        if (!initialized) {
            super.init(network, random);
            params.getMult().init(network, random);
            initialized = true;
        }
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
        return toMult.get(0);
    }
}
