package anonymous.rsa.our.sub.membership;

import anonymous.IProtocol;
import anonymous.network.NetworkException;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.List;

public interface IMembership extends IProtocol {
    Serializable execute(Serializable xShare, List<BigInteger> set, BigInteger modulo) throws NetworkException;
}
