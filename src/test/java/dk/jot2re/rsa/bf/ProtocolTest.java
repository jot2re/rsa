package dk.jot2re.rsa.bf;

import dk.jot2re.AbstractProtocolTest;
import dk.jot2re.rsa.RSATestUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ProtocolTest extends AbstractProtocolTest {
    // TODO test for more parties
    // TODO negative tests
    @ParameterizedTest
    @ValueSource(ints = {2, 3, 5})
    public void sunshine(int parties) throws Exception {
        BigInteger p = RSATestUtils.prime(DEFAULT_BIT_LENGTH, rand);
        BigInteger q = RSATestUtils.prime(DEFAULT_BIT_LENGTH, rand);
        Map<Integer, BigInteger> pShares = new HashMap<>(parties);
        Map<Integer, BigInteger>  qShares = new HashMap<>(parties);
        // We sample a number small enough to avoid issues with negative shares
        for (int party = 1; party < parties; party++) {
            pShares.put(party, (new BigInteger(DEFAULT_BIT_LENGTH - 4 - parties, rand)).multiply(BigInteger.valueOf(4)));
            qShares.put(party, (new BigInteger(DEFAULT_BIT_LENGTH - 4 - parties, rand)).multiply(BigInteger.valueOf(4)));
        }
        pShares.put(0, p.subtract(pShares.values().stream().reduce(BigInteger.ZERO, (a, b)-> a.add(b))));
        qShares.put(0, q.subtract(qShares.values().stream().reduce(BigInteger.ZERO, (a, b)-> a.add(b))));
        BigInteger N = p.multiply(q);


        RunProtocol<Boolean> protocolRunner = (param) -> {
            Protocol protocol = new Protocol((BFParameters) param);
            return protocol.execute(pShares.get(param.getMyId()), qShares.get(param.getMyId()), N);
        };

        ResultCheck<Boolean> checker = (res) -> {
            for (Future<Boolean> cur : res) {
                assertTrue(cur.get());
            }
        };

        runProtocolTest(DEFAULT_BIT_LENGTH, DEFAULT_STAT_SEC, parties, protocolRunner, checker);
    }

    // First 3 tests are from https://www.mathworks.com/help/symbolic/jacobisymbol.html
    @ParameterizedTest
    @CsvSource({"1,9,1", "28,9,1", "14,561,1", "1353,566480805,0", "1353,566480807,1","7,23,-1"})
    public void jacobiTest(int a, int n, int res) {
        assertEquals(res, Protocol.jacobiSymbol(BigInteger.valueOf(a), BigInteger.valueOf(n)));
    }
}
