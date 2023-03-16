package dk.jot2re.rsa.out.sub.invert;

import dk.jot2re.network.DummyNetwork;
import dk.jot2re.rsa.RSATestUtils;
import dk.jot2re.rsa.bf.BFParameters;
import dk.jot2re.rsa.our.sub.invert.Invert;
import dk.jot2re.rsa.our.sub.multToAdd.MultToAdd;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

public class ProtocolTest {
    private static final int DEFAULT_BIT_LENGTH = 1024;
    private static final int DEFAULT_STAT_SEC = 40;
    private static final int DEFAULT_PARTIES = 3;
    private static Map<Integer, BFParameters> params;
    private static Map<Integer, MultToAdd> multToAddMap;

    @BeforeAll
    public static void setup() throws Exception {
        params = RSATestUtils.getParameters(DEFAULT_BIT_LENGTH, DEFAULT_STAT_SEC, DEFAULT_PARTIES);
        multToAddMap = new HashMap<>(DEFAULT_PARTIES);
        for (BFParameters cur : params.values()) {
            multToAddMap.put(cur.getMyId(), new MultToAdd(cur));
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 3, 5})
    public void sunshine(int parties) throws Exception {
        int primeBits = DEFAULT_BIT_LENGTH;
        int statSec = DEFAULT_STAT_SEC;
        Random rand = new Random(42);
        DummyNetwork.TIME_OUT_MS = 100000000;
        BigInteger modulo = BigInteger.probablePrime(primeBits, rand);

        BigInteger input = BigInteger.valueOf(42);
        BigInteger refValue = input.modInverse(modulo);
        Map<Integer, BigInteger> shares = share(input, parties, modulo, rand);
        Map<Integer, BFParameters> params = RSATestUtils.getParameters(primeBits, statSec, parties);
        Map<Integer, Invert> protocols = new ConcurrentHashMap<>(parties);
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        List<Future<BigInteger>> res = new ArrayList<>(parties);
        for (int i = 0; i < parties; i++) {
            int finalI = i;
            res.add(executor.submit(() -> {
                try {
                    protocols.put(finalI, new Invert(params.get(finalI)));
                    return protocols.get(finalI).execute(shares.get(finalI), modulo);
                } catch (Exception e) {
                    System.err.println("Error: " + e.getMessage());
                    throw new RuntimeException(e);
                }
            }));
        }
        executor.shutdown();
        assertTrue(executor.awaitTermination(10000, TimeUnit.SECONDS));

        BigInteger finalValue = BigInteger.ZERO;
        for (Future<BigInteger> cur : res) {
            finalValue = finalValue.add(cur.get()).mod(modulo);
            assertNotEquals(BigInteger.ZERO, cur.get());
            assertNotEquals(BigInteger.ONE, cur.get());
        }
        assertEquals(refValue, finalValue);
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 3, 5})
    void testShare(int parties) {
        BigInteger modulo = BigInteger.TWO.pow(60);
        BigInteger ref = BigInteger.valueOf(1337);
        Map<Integer, BigInteger> shares = share(ref, parties, modulo, params.get(0).getRandom());
        assertEquals(ref, shares.values().stream().reduce(BigInteger.ZERO, (a, b) -> a.add(b).mod(modulo)));
        assertEquals(shares.size(), parties);
    }

    private static Map<Integer, BigInteger> share(BigInteger value, int parties, BigInteger modulus, Random rand) {
        Map<Integer, BigInteger> shares = new HashMap<>(parties);
        BigInteger sum = BigInteger.ZERO;
        for (int i = 1; i < parties; i++) {
            BigInteger randomNumber = new BigInteger(modulus.bitLength(), rand);
            sum = sum.add(randomNumber);
            shares.put(i, randomNumber);
        }
        // Compute pivot
        shares.put(0, value.subtract(sum).mod(modulus));
        return shares;
    }
}
