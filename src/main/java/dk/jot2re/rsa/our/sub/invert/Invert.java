package dk.jot2re.rsa.our.sub.invert;

import dk.jot2re.network.NetworkException;
import dk.jot2re.rsa.bf.BFParameters;
import dk.jot2re.rsa.our.RSAUtil;

import java.io.Serializable;
import java.math.BigInteger;

public class Invert {
    private final BFParameters params;

    public Invert(BFParameters params) {
        this.params = params;
    }

    public Serializable execute(Serializable xShare, BigInteger modulo) throws NetworkException {
        BigInteger rPart = RSAUtil.sample(params.getRandom(), modulo);
        Serializable rShare = params.getMult().shareFromAdditive(rPart, modulo);
        Serializable rxShare = params.getMult().multShares(rShare, xShare, modulo);
        BigInteger rx = params.getMult().open(rxShare, modulo);
        BigInteger y = rx.modInverse(modulo);
        return params.getMult().multConst(rShare, y, modulo);
    }
}

