package dk.jot2re.rsa.our.sub.membership;

import dk.jot2re.network.NetworkException;

import java.math.BigInteger;
import java.util.List;

public interface IMembership {
    BigInteger execute(BigInteger xShare, List<BigInteger> set, BigInteger modulo) throws NetworkException;
}
