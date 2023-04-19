package dk.jot2re.rsa.our.sub.multToAdd;

import dk.jot2re.AbstractProtocolTest;
import dk.jot2re.rsa.RSATestUtils;
import dk.jot2re.rsa.bf.BFParameters;
import dk.jot2re.rsa.our.OurParameters;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

import static dk.jot2re.DefaultSecParameters.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class MultToAddProtocolTest extends AbstractProtocolTest {
    private static Map<Integer, MultToAdd> multToAddMap;

    @BeforeAll
    public static void setup() {
        Map<Integer, BFParameters> params = RSATestUtils.getBFParameters(PRIME_BITLENGTH, STAT_SEC, DEFAULT_PARTIES);
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
            BigInteger multShare = new BigInteger(MODULO_BITLENGTH, rand).mod(MODULO);
            multShares.put(i, multShare);
            refValue = refValue.multiply(multShare).mod(MODULO);
        }

        AbstractProtocolTest.RunProtocol<BigInteger> protocolRunner = (param) -> {
            MultToAdd protocol = new MultToAdd((BFParameters) param);
            return protocol.execute(multShares.get(param.getMyId()), MODULO);
        };

        BigInteger finalRefValue = refValue;
        AbstractProtocolTest.ResultCheck<BigInteger> checker = (res) -> {
            BigInteger finalValue = BigInteger.ZERO;
            for (Future<BigInteger> cur : res) {
                finalValue = finalValue.add(cur.get()).mod(MODULO);
                assertNotEquals(BigInteger.ONE, cur.get());
                assertNotEquals(BigInteger.ZERO, cur.get());
            }
            assertEquals(finalRefValue, finalValue);
        };

        Map<Integer, OurParameters> parameters = RSATestUtils.getOurParameters(PRIME_BITLENGTH, STAT_SEC, parties);
        runProtocolTest(parameters, protocolRunner, checker);
    }

    @ParameterizedTest
    @ValueSource(ints = { 2, 3, 5})
    public void sunshineWCorrelatedRandomness(int parties) throws Exception {
        Map<Integer, BigInteger> multShares = new HashMap<>(parties);
        BigInteger refValue = BigInteger.ONE;
        for (int i = 0; i < parties; i++) {
            BigInteger multShare = new BigInteger(MODULO_BITLENGTH, rand).mod(MODULO);
            multShares.put(i, multShare);
            refValue = refValue.multiply(multShare).mod(MODULO);
        }

        AbstractProtocolTest.RunProtocol<BigInteger> protocolRunner = (param) -> {
            MultToAdd protocol = new MultToAdd((BFParameters) param);
            return protocol.executeWithCorrelatedRandomness(multShares.get(param.getMyId()), MODULO);
        };

        BigInteger finalRefValue = refValue;
        AbstractProtocolTest.ResultCheck<BigInteger> checker = (res) -> {
            BigInteger finalValue = BigInteger.ZERO;
            for (Future<BigInteger> cur : res) {
                finalValue = finalValue.add(cur.get()).mod(MODULO);
                assertNotEquals(BigInteger.ONE, cur.get());
                assertNotEquals(BigInteger.ZERO, cur.get());
            }
            assertEquals(finalRefValue, finalValue);
        };

        Map<Integer, OurParameters> parameters = RSATestUtils.getOurParameters(PRIME_BITLENGTH, STAT_SEC, parties);
        runProtocolTest(parameters, protocolRunner, checker);
    }


    @ParameterizedTest
    @ValueSource(ints = { 2, 3, 5})
    public void correlatedRandomness(int parties) throws Exception {
        Map<Integer, BigInteger> multShares = new HashMap<>(parties);
        BigInteger refValue = BigInteger.ONE;
        for (int i = 0; i < parties; i++) {
            BigInteger multShare = new BigInteger(32, rand);
            multShares.put(i, multShare);
            refValue = refValue.multiply(multShare).mod(MODULO);
        }

        AbstractProtocolTest.RunProtocol<MultToAdd.RandomShare> protocolRunner = (param) -> {
            MultToAdd protocol = new MultToAdd((BFParameters) param);
            return protocol.correlatedRandomness(MODULO);
        };

        AbstractProtocolTest.ResultCheck<MultToAdd.RandomShare> checker = (res) -> {
            BigInteger additiveRef = BigInteger.ZERO;
            BigInteger multiplicativeRef = BigInteger.ONE;
            for (Future<MultToAdd.RandomShare> cur : res) {
                additiveRef = additiveRef.add((BigInteger) cur.get().getAdditive().getRawShare()).mod(MODULO);
                multiplicativeRef = multiplicativeRef.multiply(cur.get().getMultiplicative()).mod(MODULO);
                assertNotEquals(BigInteger.ZERO, cur.get().getAdditive());
                assertNotEquals(BigInteger.ONE, cur.get().getAdditive());
                assertNotEquals(BigInteger.ZERO, cur.get().getMultiplicative());
                assertNotEquals(BigInteger.ONE, cur.get().getMultiplicative());
            }
            assertEquals(additiveRef, multiplicativeRef);
        };

        Map<Integer, BFParameters> parameters = RSATestUtils.getBFParameters(PRIME_BITLENGTH, STAT_SEC, parties);
        runProtocolTest(parameters, protocolRunner, checker);
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
