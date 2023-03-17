package dk.jot2re.rsa.out.sub.invert;

import dk.jot2re.rsa.RSATestUtils;
import dk.jot2re.rsa.bf.BFParameters;
import dk.jot2re.rsa.our.sub.invert.Invert;
import dk.jot2re.rsa.our.sub.multToAdd.MultToAdd;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
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
        BigInteger refValue = input.modInverse(modulo);
        Map<Integer, BigInteger> shares = share(input, parties, modulo, rand);

        RSATestUtils.RunProtocol<BigInteger> protocolRunner = (param) -> {
            Invert protocol = new Invert((BFParameters) param);
            return protocol.execute(shares.get(param.getMyId()), modulo);
        };

        RSATestUtils.ResultCheck<BigInteger> checker = (res) -> {
            BigInteger finalValue = BigInteger.ZERO;
            for (Future<BigInteger> cur : res) {
                finalValue = finalValue.add(cur.get()).mod(modulo);
                assertNotEquals(BigInteger.ZERO, cur.get());
                assertNotEquals(BigInteger.ONE, cur.get());
            }
            assertEquals(refValue, finalValue);
        };

        runProtocolTest(DEFAULT_BIT_LENGTH, DEFAULT_STAT_SEC, parties, protocolRunner, checker);
    }
}
