package dk.jot2re.rsa.our.sub.membership;

import dk.jot2re.network.NetworkException;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.List;

public interface IMembership {
    Serializable execute(Serializable xShare, List<BigInteger> set, BigInteger modulo) throws NetworkException;
}
