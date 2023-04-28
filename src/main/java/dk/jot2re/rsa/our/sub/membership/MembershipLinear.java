package dk.jot2re.rsa.our.sub.membership;

import dk.jot2re.AbstractProtocol;
import dk.jot2re.network.INetwork;
import dk.jot2re.network.NetworkException;
import dk.jot2re.rsa.bf.BFParameters;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.List;
import java.util.Random;

public class MembershipLinear extends AbstractProtocol implements IMembership {
    private final BFParameters params;
    private boolean initialized = false;

    public MembershipLinear(BFParameters params) {
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
        Serializable temp = params.getMult().addConst(xShare, set.get(0).negate(), modulo);
        for (int i = 1; i < set.size(); i++) {
            temp = params.getMult().multShares(temp, params.getMult().addConst(xShare, set.get(i).negate(), modulo), modulo);
        }
        return temp;
    }
}
