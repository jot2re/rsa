package dk.jot2re.rsa.bf;

import dk.jot2re.rsa.RSATestUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ProtocolTest {

    // TODO test for more parties
    // TODO negative tests
    @Test
    public void sunshine() throws Exception {
        int primeBits = 1024;
        int statSec = 40;
        int parties = 3;
        Random rand = new Random(42);
        BigInteger p = RSATestUtils.prime(primeBits, rand);
        BigInteger q = RSATestUtils.prime(primeBits, rand);
        Map<Integer, BigInteger> pShares = new HashMap<>(parties);
        Map<Integer, BigInteger>  qShares = new HashMap<>(parties);
        // We sample a number small enough to avoid issues with negative shares
        for (int party = 1; party < parties; party++) {
            pShares.put(party, (new BigInteger(primeBits - 4 - parties, rand)).multiply(BigInteger.valueOf(4)));
            qShares.put(party, (new BigInteger(primeBits - 4 - parties, rand)).multiply(BigInteger.valueOf(4)));
        }
        pShares.put(0, p.subtract(pShares.values().stream().reduce(BigInteger.ZERO, (a, b)-> a.add(b))));
        qShares.put(0, q.subtract(qShares.values().stream().reduce(BigInteger.ZERO, (a, b)-> a.add(b))));
        BigInteger N = p.multiply(q);
        Map<Integer, BFParameters> params = RSATestUtils.getParameters(primeBits, statSec, parties);
        Map<Integer, Protocol> protocols = new ConcurrentHashMap<>(parties);
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        List<Future<Boolean>> res = new ArrayList<>(parties);
        for (int i = 0; i < parties; i++) {
            int finalI = i;
            res.add(executor.submit(() -> {
                try {
                    protocols.put(finalI, new Protocol(params.get(finalI)));
                    return protocols.get(finalI).execute(pShares.get(finalI), qShares.get(finalI), N);
                } catch (Exception e) {
                    System.err.println("Error: " + e.getMessage());
                    throw new RuntimeException(e);
                }
            }));
        }
        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.MINUTES));

        for (Future<Boolean> cur : res) {
            assertTrue(cur.get());
        }
    }

    // First 3 tests are from https://www.mathworks.com/help/symbolic/jacobisymbol.html
    @ParameterizedTest
    @CsvSource({"1,9,1", "28,9,1", "14,561,1", "1353,566480805,0", "1353,566480807,1","7,23,-1"})
    public void jacobiTest(int a, int n, int res) {
        assertEquals(res, Protocol.jacobiSymbol(BigInteger.valueOf(a), BigInteger.valueOf(n)));
    }
}
