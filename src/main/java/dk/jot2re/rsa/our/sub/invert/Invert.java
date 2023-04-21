package dk.jot2re.rsa.our.sub.invert;

import dk.jot2re.AbstractProtocol;
import dk.jot2re.network.INetwork;
import dk.jot2re.network.NetworkException;
import dk.jot2re.rsa.bf.BFParameters;
import dk.jot2re.rsa.our.RSAUtil;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.Random;

public class Invert extends AbstractProtocol {
    private final BFParameters params;

    public Invert(BFParameters params) {
        this.params = params;
    }

    @Override
    public void init(INetwork network, Random random) {
        super.init(network, random);
        params.getMult().init(network, random);
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

