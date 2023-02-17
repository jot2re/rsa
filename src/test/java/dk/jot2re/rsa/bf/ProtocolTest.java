package dk.jot2re.rsa.bf;

import dk.jot2re.rsa.RSATestUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ProtocolTest {

    // TODO test for more parties
    // TODO negative tests
    @Test
    public void sunshine() throws Exception {
        int primeBits = 1024;
        int statSec = 40;
        Random rand = new Random(42);
        BigInteger p = RSATestUtils.prime(primeBits, rand);
        BigInteger q = RSATestUtils.prime(primeBits, rand);
        List<BigInteger> pShares = new ArrayList<>(2);
        List<BigInteger> qShares = new ArrayList<>(2);
        pShares.add((new BigInteger(primeBits-4, rand)).multiply(BigInteger.valueOf(4)).add(BigInteger.valueOf(3)));
        pShares.add(p.subtract(pShares.get(0)));
        qShares.add((new BigInteger(primeBits-4, rand)).multiply(BigInteger.valueOf(4)).add(BigInteger.valueOf(3)));
        qShares.add(q.subtract(qShares.get(0)));
        BigInteger N = p.multiply(q);
        Map<Integer, BFParameters> params = RSATestUtils.getParameters(primeBits, statSec, 2);
        Map<Integer, Protocol> protocols = new HashMap<>(2);
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        List<Future<Boolean>> res = new ArrayList<>(2);
        for (int i = 0; i < 2; i++) {
            int finalI = i;
            res.add(executor.submit(() -> {
                try {
                    protocols.put(finalI, new Protocol(params.get(finalI)));
                    return protocols.get(finalI).execute(pShares.get(finalI), qShares.get(finalI), N);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }));
        }
        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));

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
