package dk.jot2re.rsa.our.sub.invert;

import dk.jot2re.AbstractProtocolTest;
import dk.jot2re.network.INetwork;
import dk.jot2re.rsa.RSATestUtils;
import dk.jot2re.rsa.bf.BFParameters;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.Map;
import java.util.concurrent.Future;

import static dk.jot2re.DefaultSecParameters.*;
import static dk.jot2re.rsa.RSATestUtils.share;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class InvertProtocolTest extends AbstractProtocolTest {
    @ParameterizedTest
    @ValueSource(ints = {2, 3, 5})
    public void sunshine(int parties) throws Exception {
        BigInteger input = BigInteger.valueOf(42);
        BigInteger refValue = input.modInverse(MODULO);
        Map<Integer, BigInteger> shares = share(input, parties, MODULO, rand);

        RunProtocol<BigInteger> protocolRunner = (param, network) -> {
            Invert protocol = new Invert((BFParameters) param);
            protocol.init(network, RSATestUtils.getRandom(network.myId()));
            Serializable myShare = ((BFParameters) param).getMult().shareFromAdditive(shares.get(network.myId()), MODULO);
            Serializable res = protocol.execute(myShare, MODULO);
            return ((BFParameters) param).getMult().combineToAdditive(res, MODULO);
        };

        ResultCheck<BigInteger> checker = (res) -> {
            BigInteger finalValue = BigInteger.ZERO;
            for (Future<BigInteger> cur : res) {
                finalValue = finalValue.add(cur.get()).mod(MODULO);
//                assertNotEquals(BigInteger.ZERO, cur.get());
                assertNotEquals(BigInteger.ONE, cur.get());
            }
            assertEquals(refValue, finalValue);
        };

        Map<Integer, BFParameters> parameters = RSATestUtils.getBFParameters(PRIME_BITLENGTH, STAT_SEC, parties);
        Map<Integer, INetwork> networks = RSATestUtils.getNetworks(parties);
        runProtocolTest(networks, parameters, protocolRunner, checker);
    }
}
