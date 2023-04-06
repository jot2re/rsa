package dk.jot2re.rsa.bf;

import dk.jot2re.AbstractProtocolTest;
import dk.jot2re.rsa.RSATestUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigInteger;
import java.util.Map;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BFProtocolTest extends AbstractProtocolTest {
    // TODO negative tests
    @ParameterizedTest
    @ValueSource(ints = {2, 3, 5})
    public void sunshine(int parties) throws Exception {
        Map<Integer, BigInteger> pShares = RSATestUtils.randomPrime(parties, DEFAULT_BIT_LENGTH, rand);
        Map<Integer, BigInteger> qShares = RSATestUtils.randomPrime(parties, DEFAULT_BIT_LENGTH, rand);
        BigInteger p = pShares.values().stream().reduce(BigInteger.ZERO, (a, b)-> a.add(b));
        BigInteger q = qShares.values().stream().reduce(BigInteger.ZERO, (a, b)-> a.add(b));
        BigInteger N = p.multiply(q);

        RunProtocol<Boolean> protocolRunner = (param) -> {
            BFProtocol protocol = new BFProtocol((BFParameters) param);
            if (!protocol.validateParameters(pShares.get(param.getMyId()), qShares.get(param.getMyId()), N)) {
                return false;
            }
            long start = System.currentTimeMillis();
            boolean res = protocol.execute(pShares.get(param.getMyId()), qShares.get(param.getMyId()), N);
            long stop = System.currentTimeMillis();
            System.out.println("time: " + (stop-start));
            return res;
        };

        ResultCheck<Boolean> checker = (res) -> {
            for (Future<Boolean> cur : res) {
                assertTrue(cur.get());
            }
        };

        Map<Integer, BFParameters> parameters = RSATestUtils.getBFParameters(DEFAULT_BIT_LENGTH, DEFAULT_STAT_SEC, parties);
        runProtocolTest(parameters, protocolRunner, checker);
    }

    // First 3 tests are from https://www.mathworks.com/help/symbolic/jacobisymbol.html
    @ParameterizedTest
    @CsvSource({"1,9,1", "28,9,1", "14,561,1", "1353,566480805,0", "1353,566480807,1","7,23,-1"})
    public void jacobiTest(int a, int n, int res) {
        assertEquals(res, BFProtocol.jacobiSymbol(BigInteger.valueOf(a), BigInteger.valueOf(n)));
    }
}
