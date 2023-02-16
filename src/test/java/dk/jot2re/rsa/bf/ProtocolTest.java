package dk.jot2re.rsa.bf;

import dk.jot2re.network.DummyNetwork;
import dk.jot2re.network.DummyNetworkFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ProtocolTest {
    private Map<Integer, BFParameters> getParameters(int bits, int statSec, int parties) throws Exception {
        DummyNetworkFactory factory = new DummyNetworkFactory(parties);
        Map<Integer, DummyNetwork> networks = factory.getNetworks();
        Map<Integer, BFParameters> params = new HashMap<>(parties);
        for (DummyNetwork network : networks.values()) {
            // Unique but deterministic seed for each set of parameters
            SecureRandom rand = SecureRandom.getInstance("SHA1PRNG", "SUN");
            rand.setSeed((byte)network.myId());
            params.put(network.myId(), new BFParameters(bits, statSec, network, rand));
        }
        return params;
    }
    private BigInteger prime(int bits, Random rand) {
        BigInteger cand;
        do {
            cand = BigInteger.probablePrime(bits, rand);
        } while (!cand.mod(BigInteger.valueOf(4)).equals(BigInteger.valueOf(3)));
        return cand;
    }
    @Test
    public void sunshine() throws Exception {
        int primeBits = 1024;
        int statSec = 40;
        DummyNetwork.TIME_OUT_MS = 100000000;
        Random rand = new Random(42);
        BigInteger p = prime(primeBits, rand);
        BigInteger q = prime(primeBits, rand);
        List<BigInteger> pShares = new ArrayList<>(2);
        List<BigInteger> qShares = new ArrayList<>(2);
        pShares.add((new BigInteger(primeBits-4, rand)).multiply(BigInteger.valueOf(4)).add(BigInteger.valueOf(3)));
        pShares.add(p.subtract(pShares.get(0)));
        qShares.add((new BigInteger(primeBits-4, rand)).multiply(BigInteger.valueOf(4)).add(BigInteger.valueOf(3)));
        qShares.add(q.subtract(qShares.get(0)));
        BigInteger N = p.multiply(q);
        Map<Integer, BFParameters> params = getParameters(primeBits, statSec, 2);
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
