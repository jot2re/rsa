package anonymous.rsa.our.sub.multToAdd;

import anonymous.AbstractProtocolTest;
import anonymous.rsa.RSATestUtils;
import anonymous.network.INetwork;
import anonymous.rsa.bf.BFParameters;
import anonymous.rsa.our.OurParameters;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

import static anonymous.DefaultSecParameters.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class MultToAddProtocolTest extends AbstractProtocolTest {
    private static Map<Integer, MultToAdd> multToAddMap;

    @BeforeAll
    public static void setup() {
        Map<Integer, BFParameters> params = RSATestUtils.getBFParameters(PRIME_BITLENGTH, STAT_SEC, DEFAULT_PARTIES, false);
        Map<Integer, INetwork> nets = RSATestUtils.getNetworks(DEFAULT_PARTIES);
        multToAddMap = new HashMap<>(DEFAULT_PARTIES);
        for (int i = 0; i < params.size(); i++) {
            MultToAdd mta =new MultToAdd(params.get(i));
            mta.init(nets.get(i), RSATestUtils.getRandom(i));
            multToAddMap.put(i, mta);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = { 2, 3, 5})
    public void sunshine(int parties) throws Exception {
        Map<Integer, BigInteger> multShares = new HashMap<>(parties);
        BigInteger refValue = BigInteger.ONE;
        for (int i = 0; i < parties; i++) {
            BigInteger multShare = new BigInteger(MODULO_BITLENGTH, rand);
            multShares.put(i, multShare);
            refValue = refValue.multiply(multShare).mod(MODULO);
        }

        AbstractProtocolTest.RunProtocol<BigInteger> protocolRunner = (param, network) -> {
            MultToAdd protocol = new MultToAdd((BFParameters) param);
            protocol.init(network, RSATestUtils.getRandom(network.myId()));
            return protocol.execute(multShares.get(network.myId()), MODULO);
        };

        BigInteger finalRefValue = refValue;
        AbstractProtocolTest.ResultCheck<BigInteger> checker = (res) -> {
            BigInteger finalValue = BigInteger.ZERO;
            for (Future<BigInteger> cur : res) {
                finalValue = finalValue.add(cur.get()).mod(MODULO);
                assertNotEquals(BigInteger.ONE, cur.get());
//                assertNotEquals(BigInteger.ZERO, cur.get());
            }
            assertEquals(finalRefValue, finalValue);
        };

        Map<Integer, OurParameters> parameters = RSATestUtils.getOurParameters(PRIME_BITLENGTH, STAT_SEC, parties);
        Map<Integer, INetwork> networks = RSATestUtils.getNetworks(parties);
        runProtocolTest(networks, parameters, protocolRunner, checker);
    }

    @ParameterizedTest
    @ValueSource(ints = { 2, 3, 5})
    public void sunshineWCorrelatedRandomness(int parties) throws Exception {
        Map<Integer, BigInteger> multShares = new HashMap<>(parties);
        BigInteger refValue = BigInteger.ONE;
        for (int i = 0; i < parties; i++) {
            BigInteger multShare = new BigInteger(MODULO_BITLENGTH, rand);
            multShares.put(i, multShare);
            refValue = refValue.multiply(multShare).mod(MODULO);
        }

        AbstractProtocolTest.RunProtocol<BigInteger> protocolRunner = (param, network) -> {
            MultToAdd protocol = new MultToAdd((BFParameters) param);
            protocol.init(network, RSATestUtils.getRandom(network.myId()));
            return protocol.executeWithCorrelatedRandomness(multShares.get(network.myId()), MODULO);
        };

        BigInteger finalRefValue = refValue;
        AbstractProtocolTest.ResultCheck<BigInteger> checker = (res) -> {
            BigInteger finalValue = BigInteger.ZERO;
            for (Future<BigInteger> cur : res) {
                finalValue = finalValue.add(cur.get()).mod(MODULO);
                assertNotEquals(BigInteger.ONE, cur.get());
//                assertNotEquals(BigInteger.ZERO, cur.get());
            }
            assertEquals(finalRefValue, finalValue);
        };

        Map<Integer, OurParameters> parameters = RSATestUtils.getOurParameters(PRIME_BITLENGTH, STAT_SEC, parties);
        Map<Integer, INetwork> networks = RSATestUtils.getNetworks(parties);
        runProtocolTest(networks, parameters, protocolRunner, checker);
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
