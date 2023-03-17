package dk.jot2re.rsa.our.sub.membership;

import dk.jot2re.rsa.RSATestUtils;
import dk.jot2re.rsa.bf.BFParameters;
import dk.jot2re.rsa.our.sub.multToAdd.MultToAdd;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.Future;

import static dk.jot2re.rsa.RSATestUtils.runProtocolTest;
import static dk.jot2re.rsa.RSATestUtils.share;
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
        // NOTE: ENABLE FOR DEBUGGING
        //        DummyNetwork.TIME_OUT_MS = 100000000;
        params = RSATestUtils.getParameters(DEFAULT_BIT_LENGTH, DEFAULT_STAT_SEC, DEFAULT_PARTIES);
        multToAddMap = new HashMap<>(DEFAULT_PARTIES);
        for (BFParameters cur : params.values()) {
            multToAddMap.put(cur.getMyId(), new MultToAdd(cur));
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 3, 5})
    public void sunshine(int parties) throws Exception {
        BigInteger input = BigInteger.valueOf(42);
        List<BigInteger> set = Arrays.asList(BigInteger.valueOf(12), BigInteger.valueOf(2544), BigInteger.valueOf(42), BigInteger.valueOf(1000));
        Map<Integer, BigInteger> shares = share(input, parties, modulo, rand);

        RSATestUtils.RunProtocol<BigInteger> protocolRunner = (param) -> {
            MembershipLog protocol = new MembershipLog((BFParameters) param);
            return protocol.execute(shares.get(param.getMyId()), set, modulo);
        };

        RSATestUtils.ResultCheck<BigInteger> checker = (res) -> {
            BigInteger finalValue = BigInteger.ZERO;
            for (Future<BigInteger> cur : res) {
                finalValue = finalValue.add(cur.get()).mod(modulo);
                assertNotEquals(BigInteger.ONE, cur.get());
            }
            // Ensure the result is 0
            assertEquals(BigInteger.ZERO, finalValue);
        };

        runProtocolTest(DEFAULT_BIT_LENGTH, DEFAULT_STAT_SEC, parties, protocolRunner, checker);
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 3, 5})
    public void negative(int parties) throws Exception {
        BigInteger input = BigInteger.valueOf(42);
        List<BigInteger> set = Arrays.asList(BigInteger.valueOf(12), BigInteger.valueOf(2544), BigInteger.valueOf(4), BigInteger.valueOf(1000));
        Map<Integer, BigInteger> shares = share(input, parties, modulo, rand);

        RSATestUtils.RunProtocol<BigInteger> protocolRunner = (param) -> {
            MembershipLog protocol = new MembershipLog((BFParameters) param);
            return protocol.execute(shares.get(param.getMyId()), set, modulo);
        };

        RSATestUtils.ResultCheck<BigInteger> checker = (res) -> {
            BigInteger finalValue = BigInteger.ZERO;
            for (Future<BigInteger> cur : res) {
                finalValue = finalValue.add(cur.get()).mod(modulo);
                assertNotEquals(BigInteger.ONE, cur.get());
            }
            // Ensure the result is NOT 0
            assertNotEquals(BigInteger.ZERO, finalValue);
        };

        runProtocolTest(DEFAULT_BIT_LENGTH, DEFAULT_STAT_SEC, parties, protocolRunner, checker);
    }

}
