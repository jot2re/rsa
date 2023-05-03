package anonymous.rsa.our.sub.invert;

import anonymous.AbstractProtocol;
import anonymous.network.INetwork;
import anonymous.network.NetworkException;
import anonymous.rsa.bf.BFParameters;
import anonymous.rsa.our.RSAUtil;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.Random;

public class Invert extends AbstractProtocol {
    private final BFParameters params;
    private boolean initialized = false;

    public Invert(BFParameters params) {
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

    public Serializable execute(Serializable xShare, BigInteger modulo) throws NetworkException {
        BigInteger rPart = RSAUtil.sample(random, modulo);
        Serializable rShare = params.getMult().shareFromAdditive(rPart, modulo);
        Serializable rxShare = params.getMult().multShares(rShare, xShare, modulo);
        BigInteger rx = params.getMult().open(rxShare, modulo);
        BigInteger y = rx.modInverse(modulo);
        return params.getMult().multConst(rShare, y, modulo);
    }
}

