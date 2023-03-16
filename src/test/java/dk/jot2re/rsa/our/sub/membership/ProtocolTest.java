package dk.jot2re.rsa.our.sub.membership;

import dk.jot2re.network.DummyNetwork;
import dk.jot2re.rsa.RSATestUtils;
import dk.jot2re.rsa.bf.BFParameters;
import dk.jot2re.rsa.our.sub.multToAdd.MultToAdd;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.*;

import static dk.jot2re.rsa.RSATestUtils.share;
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
        List<BigInteger> set = Arrays.asList(BigInteger.valueOf(12), BigInteger.valueOf(2544), BigInteger.valueOf(42), BigInteger.valueOf(1000));
        Map<Integer, BigInteger> shares = share(input, parties, modulo, rand);
        Map<Integer, BFParameters> params = RSATestUtils.getParameters(primeBits, statSec, parties);
        Map<Integer, MembershipLinear> protocols = new ConcurrentHashMap<>(parties);
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        List<Future<BigInteger>> res = new ArrayList<>(parties);
        for (int i = 0; i < parties; i++) {
            int finalI = i;
            res.add(executor.submit(() -> {
                try {
                    protocols.put(finalI, new MembershipLinear(params.get(finalI)));
                    return protocols.get(finalI).execute(shares.get(finalI), set, modulo);
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
            assertNotEquals(BigInteger.ONE, cur.get());
        }
        // Zero indicates membership, any other value indicates it is not a member of the set
        assertEquals(BigInteger.ZERO, finalValue);
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 3, 5})
    public void negative(int parties) throws Exception {
        int primeBits = DEFAULT_BIT_LENGTH;
        int statSec = DEFAULT_STAT_SEC;
        Random rand = new Random(42);
        DummyNetwork.TIME_OUT_MS = 100000000;
        BigInteger modulo = BigInteger.probablePrime(primeBits, rand);

        BigInteger input = BigInteger.valueOf(42);
        List<BigInteger> set = Arrays.asList(BigInteger.valueOf(12), BigInteger.valueOf(2544), BigInteger.valueOf(41), BigInteger.valueOf(1000));
        Map<Integer, BigInteger> shares = share(input, parties, modulo, rand);
        Map<Integer, BFParameters> params = RSATestUtils.getParameters(primeBits, statSec, parties);
        Map<Integer, MembershipLinear> protocols = new ConcurrentHashMap<>(parties);
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        List<Future<BigInteger>> res = new ArrayList<>(parties);
        for (int i = 0; i < parties; i++) {
            int finalI = i;
            res.add(executor.submit(() -> {
                try {
                    protocols.put(finalI, new MembershipLinear(params.get(finalI)));
                    return protocols.get(finalI).execute(shares.get(finalI), set, modulo);
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
            assertNotEquals(BigInteger.ONE, cur.get());
        }
        assertNotEquals(BigInteger.ZERO, finalValue);
    }

}
