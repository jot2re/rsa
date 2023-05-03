package anonymous.rsa.bf;

import anonymous.AbstractProtocolTest;
import anonymous.network.DummyNetwork;
import anonymous.network.INetwork;
import anonymous.rsa.RSATestUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigInteger;
import java.util.Map;
import java.util.concurrent.Future;

import static anonymous.DefaultSecParameters.PRIME_BITLENGTH;
import static anonymous.DefaultSecParameters.STAT_SEC;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BFProtocolTest extends AbstractProtocolTest {
    // TODO negative tests
    @ParameterizedTest
    @ValueSource(ints = {2, 3, 5})
    public void sunshine(int parties) throws Exception {
        Map<Integer, BigInteger> pShares = RSATestUtils.randomPrime(parties, PRIME_BITLENGTH, rand);
        Map<Integer, BigInteger> qShares = RSATestUtils.randomPrime(parties, PRIME_BITLENGTH, rand);
        BigInteger p = pShares.values().stream().reduce(BigInteger.ZERO, (a, b)-> a.add(b));
        BigInteger q = qShares.values().stream().reduce(BigInteger.ZERO, (a, b)-> a.add(b));
        BigInteger N = p.multiply(q);

        RunProtocol<Boolean> protocolRunner = (param, network) -> {
            BFProtocol protocol = new BFProtocol((BFParameters) param);
            protocol.init(network, RSATestUtils.getRandom(network.myId()));
            if (!protocol.validateParameters(pShares.get(network.myId()), qShares.get(network.myId()), N)) {
                return false;
            }
            long start = System.currentTimeMillis();
            boolean res = protocol.execute(pShares.get(network.myId()), qShares.get(network.myId()), N);
            long stop = System.currentTimeMillis();
            System.out.println("time: " + (stop-start));
            return res;
        };

        ResultCheck<Boolean> checker = (res) -> {
            for (Future<Boolean> cur : res) {
                assertTrue(cur.get());
            }
        };

        Map<Integer, BFParameters> parameters = RSATestUtils.getBFParameters(PRIME_BITLENGTH, STAT_SEC, parties);
        Map<Integer, INetwork> networks = RSATestUtils.getNetworks(parties);
        runProtocolTest(networks, parameters, protocolRunner, checker);
//        System.out.println("Mult calls " + ((DummyMult) parameters.get(0).getMult()).getMultCalls());
        System.out.println("Rounds " + ((DummyNetwork) networks.get(0)).getRounds());
        System.out.println("Nettime " + ((DummyNetwork) networks.get(0)).getNetworkTime());
        System.out.println("Nettrans " + ((DummyNetwork) networks.get(0)).getTransfers());
        System.out.println("Net bytes " + ((DummyNetwork) networks.get(0)).getBytesSent());
    }

    // First 3 tests are from https://www.mathworks.com/help/symbolic/jacobisymbol.html
    @ParameterizedTest
    @CsvSource({"1,9,1", "28,9,1", "14,561,1", "1353,566480805,0", "1353,566480807,1","7,23,-1"})
    public void jacobiTest(int a, int n, int res) {
        assertEquals(res, BFProtocol.jacobiSymbol(BigInteger.valueOf(a), BigInteger.valueOf(n)));
    }
}
