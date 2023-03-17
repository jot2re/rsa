package dk.jot2re.rsa.our.sub.multToAdd;

import dk.jot2re.rsa.RSATestUtils;
import dk.jot2re.rsa.bf.BFParameters;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Future;

import static dk.jot2re.rsa.RSATestUtils.runProtocolTest;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class ProtocolTest {
    private static final int DEFAULT_BIT_LENGTH = 1024;
    private static final int DEFAULT_STAT_SEC = 40;
    private static final int DEFAULT_PARTIES = 3;
    private static final Random rand = new Random(42);
    private static final BigInteger modulo = BigInteger.probablePrime(DEFAULT_BIT_LENGTH, rand);
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
        Map<Integer, BigInteger> multShares = new HashMap<>(parties);
        BigInteger refValue = BigInteger.ONE;
        for (int i = 0; i < parties; i++) {
            BigInteger multShare = new BigInteger(DEFAULT_BIT_LENGTH, rand);
            multShares.put(i, multShare);
            refValue = refValue.multiply(multShare).mod(modulo);
        }

        RSATestUtils.RunProtocol<BigInteger> protocolRunner = (param) -> {
            MultToAdd protocol = new MultToAdd((BFParameters) param);
            return protocol.execute(multShares.get(param.getMyId()), modulo);
        };

        BigInteger finalRefValue = refValue;
        RSATestUtils.ResultCheck<BigInteger> checker = (res) -> {
            BigInteger finalValue = BigInteger.ZERO;
            for (Future<BigInteger> cur : res) {
                finalValue = finalValue.add(cur.get()).mod(modulo);
                assertNotEquals(BigInteger.ONE, cur.get());
                assertNotEquals(BigInteger.ZERO, cur.get());
            }
            assertEquals(finalRefValue, finalValue);
        };

        runProtocolTest(DEFAULT_BIT_LENGTH, DEFAULT_STAT_SEC, parties, protocolRunner, checker);
    }

    @ParameterizedTest
    @ValueSource(ints = { 2, 3, 5})
    public void correlatedRandomness(int parties) throws Exception {
        Map<Integer, BigInteger> multShares = new HashMap<>(parties);
        BigInteger refValue = BigInteger.ONE;
        for (int i = 0; i < parties; i++) {
            BigInteger multShare = new BigInteger(32, rand);
            multShares.put(i, multShare);
            refValue = refValue.multiply(multShare).mod(modulo);
        }

        RSATestUtils.RunProtocol<MultToAdd.RandomShare> protocolRunner = (param) -> {
            MultToAdd protocol = new MultToAdd((BFParameters) param);
            return protocol.correlatedRandomness(modulo);
        };

        RSATestUtils.ResultCheck<MultToAdd.RandomShare> checker = (res) -> {
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
        };

        runProtocolTest(DEFAULT_BIT_LENGTH, DEFAULT_STAT_SEC, parties, protocolRunner, checker);
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
