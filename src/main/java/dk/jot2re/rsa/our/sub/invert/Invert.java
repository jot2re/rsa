package dk.jot2re.rsa.our.sub.invert;

import dk.jot2re.network.NetworkException;
import dk.jot2re.rsa.bf.BFParameters;
import dk.jot2re.rsa.our.RSAUtil;

import java.math.BigInteger;

public class Invert {
    private final BFParameters params;

    public Invert(BFParameters params) {
        this.params = params;
    }

    public boolean execute(BigInteger modulo) throws NetworkException {
        BigInteger rShare = RSAUtil.sample(params, modulo);
        return false;
    }
}

