package dk.jot2re.rsa.our.sub.multToAdd;

import dk.jot2re.network.DummyNetwork;
import dk.jot2re.rsa.RSATestUtils;
import dk.jot2re.rsa.bf.BFParameters;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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
    @ValueSource(ints = { 2, 3, 5})
    public void sunshine(int parties) throws Exception {
        int primeBits = DEFAULT_BIT_LENGTH;
        int statSec = DEFAULT_STAT_SEC;
        Random rand = new Random(42);
        DummyNetwork.TIME_OUT_MS = 100000000;
        BigInteger modulo = BigInteger.probablePrime(primeBits, rand);
        Map<Integer, BigInteger> multShares = new HashMap<>(parties);
        BigInteger refValue = BigInteger.ONE;
        for (int i = 0; i < parties; i++) {
            BigInteger multShare = new BigInteger(primeBits, rand);
            multShares.put(i, multShare);
            refValue = refValue.multiply(multShare).mod(modulo);
        }
        Map<Integer, BFParameters> params = RSATestUtils.getParameters(primeBits, statSec, parties);
        Map<Integer, MultToAdd> protocols = new HashMap<>(parties);
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        List<Future<BigInteger>> res = new ArrayList<>(parties);
        for (int i = 0; i < parties; i++) {
            int finalI = i;
            res.add(executor.submit(() -> {
                try {
                    protocols.put(finalI, new MultToAdd(params.get(finalI)));
                    return protocols.get(finalI).execute(multShares.get(finalI), modulo);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }));
        }
        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));

        BigInteger finalValue = BigInteger.ZERO;
        for (Future<BigInteger> cur : res) {
            finalValue = finalValue.add(cur.get()).mod(modulo);
            assertNotEquals(BigInteger.ZERO, cur.get());
            assertNotEquals(BigInteger.ONE, cur.get());
        }
        assertEquals(refValue, finalValue);
    }

    @ParameterizedTest
    @ValueSource(ints = { 2, 3, 5})
    public void correlatedRandomness(int parties) throws Exception {
        int primeBits = 32;
        int statSec = 40;
        Random rand = new Random(42);
        BigInteger modulo = BigInteger.probablePrime(primeBits, rand);
        Map<Integer, BFParameters> params = RSATestUtils.getParameters(primeBits, statSec, parties);
        Map<Integer, MultToAdd> protocols = new HashMap<>(parties);
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        List<Future<MultToAdd.RandomShare>> res = new ArrayList<>(parties);
        for (int i = 0; i < parties; i++) {
            int finalI = i;
            res.add(executor.submit(() -> {
                try {
                    protocols.put(finalI, new MultToAdd(params.get(finalI)));
                    return protocols.get(finalI).correlatedRandomness(modulo);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }));
        }
        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));

        BigInteger additiveRef = BigInteger.ZERO;
        BigInteger multiplicativeRef = BigInteger.ONE;
        for (Future<MultToAdd.RandomShare> cur : res) {
            additiveRef = additiveRef.add(cur.get().getAdditive()).mod(modulo);
            multiplicativeRef = multiplicativeRef.multiply(cur.get().getMultiplicative()).mod(modulo);
            assertNotEquals(BigInteger.ZERO, cur.get().getAdditive());
            assertNotEquals(BigInteger.ONE, cur.get().getAdditive());
            assertNotEquals(BigInteger.ZERO, cur.get().getMultiplicative());
            assertNotEquals(BigInteger.ONE, cur.get().getMultiplicative());
        }
        assertEquals(additiveRef, multiplicativeRef);
    }

    @ParameterizedTest
    @CsvSource({"5,58,35","16486746581,31,23","44046432847841327,259854648476,214510084555"})
    public void inverseTest(long val, long modulo, long ref) {
        Map<Integer, BigInteger> res = multToAddMap.get(0).inverse(BigInteger.valueOf(val), BigInteger.valueOf(modulo));
        BigInteger cand = res.values().stream().reduce(BigInteger.ZERO, (a,b) -> a.add(b).mod(BigInteger.valueOf(modulo)));
        assertEquals(BigInteger.valueOf(ref), cand);
        // Sanity checks
        assertNotEquals(res.get(0), res.get(1));
    }
}
