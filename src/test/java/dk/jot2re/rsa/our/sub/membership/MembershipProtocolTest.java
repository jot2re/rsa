package dk.jot2re.rsa.our.sub.membership;

import dk.jot2re.AbstractProtocolTest;
import dk.jot2re.network.INetwork;
import dk.jot2re.rsa.RSATestUtils;
import dk.jot2re.rsa.bf.BFParameters;
import dk.jot2re.rsa.our.OurParameters;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import static dk.jot2re.DefaultSecParameters.*;
import static dk.jot2re.rsa.RSATestUtils.share;
import static org.junit.jupiter.api.Assertions.*;

public class MembershipProtocolTest extends AbstractProtocolTest {

    @ParameterizedTest
    @CsvSource({"2,linear", "3,linear", "5,linear", "2,log", "3,log", "5,log","2,const", "3,const", "5,const"})
    public void sunshine(int parties, String type) throws Exception {
        BigInteger input = BigInteger.valueOf(42);
        List<BigInteger> set = Arrays.asList(BigInteger.valueOf(34535), BigInteger.valueOf(5534315), BigInteger.valueOf(42), BigInteger.valueOf(890637));
        Map<Integer, BigInteger> shares = share(input, parties, MODULO, rand);

        AbstractProtocolTest.RunProtocol<BigInteger> protocolRunner = (param, network) -> {
            IMembership protocol = switch (type) {
                case "linear" -> new MembershipLinear((BFParameters) param);
                case "log" -> new MembershipLog((BFParameters) param);
                case "const" -> new MembershipConst((BFParameters) param);
                default -> throw new RuntimeException("unknown type");
            };
            protocol.init(network, RSATestUtils.getRandom(network.myId()));
            Serializable myShare = ((BFParameters) param).getMult().shareFromAdditive(shares.get(network.myId()), MODULO);
            Serializable res = protocol.execute(myShare, set, MODULO);
            return ((BFParameters) param).getMult().combineToAdditive(res, MODULO);
        };

        AbstractProtocolTest.ResultCheck<BigInteger> checker = (res) -> {
            BigInteger finalValue = BigInteger.ZERO;
            for (Future<BigInteger> cur : res) {
                finalValue = finalValue.add(cur.get()).mod(MODULO);
                assertNotEquals(BigInteger.ONE, cur.get());
            }
            // Ensure the result is 0
            assertEquals(BigInteger.ZERO, finalValue);
        };

        Map<Integer, BFParameters> parameters = RSATestUtils.getBFParameters(PRIME_BITLENGTH, STAT_SEC, parties);
        Map<Integer, INetwork> networks = RSATestUtils.getNetworks(parties);
        runProtocolTest(networks, parameters, protocolRunner, checker);
    }

    @Test
    public void oneSetConst() throws Exception {
        BigInteger mod = BigInteger.valueOf(132432449);
        BigInteger input = BigInteger.valueOf(42);
        List<BigInteger> set = Arrays.asList(BigInteger.valueOf(42));
        Map<Integer, BigInteger> shares = share(input, DEFAULT_PARTIES, mod, rand);

        AbstractProtocolTest.RunProtocol<BigInteger> protocolRunner = (param, network) -> {
            IMembership protocol = new MembershipConst((BFParameters) param);
            protocol.init(network, RSATestUtils.getRandom(network.myId()));
            Serializable myShare = ((BFParameters) param).getMult().shareFromAdditive(shares.get(network.myId()), mod);
            Serializable res = protocol.execute(myShare, set, mod);
            return ((BFParameters) param).getMult().combineToAdditive(res, mod);
        };

        AbstractProtocolTest.ResultCheck<BigInteger> checker = (res) -> {
            BigInteger finalValue = BigInteger.ZERO;
            for (Future<BigInteger> cur : res) {
                finalValue = finalValue.add(cur.get()).mod(mod);
                assertNotEquals(BigInteger.ONE, cur.get());
            }
            // Ensure the result is 0
            assertEquals(BigInteger.ZERO, finalValue);
        };

        Map<Integer, OurParameters> parameters = RSATestUtils.getOurParameters(PRIME_BITLENGTH, STAT_SEC, DEFAULT_PARTIES);
        Map<Integer, INetwork> networks = RSATestUtils.getNetworks(DEFAULT_PARTIES);
        runProtocolTest(networks, parameters, protocolRunner, checker);
    }

    @Test
    public void twoSetConst() throws Exception {
        BigInteger mod = BigInteger.valueOf(132432449);
        BigInteger input = BigInteger.valueOf(42);
        List<BigInteger> set = Arrays.asList(BigInteger.valueOf(42), BigInteger.valueOf(43));
        Map<Integer, BigInteger> shares = share(input, DEFAULT_PARTIES, mod, rand);

        AbstractProtocolTest.RunProtocol<BigInteger> protocolRunner = (param, network) -> {
            IMembership protocol = new MembershipConst((BFParameters) param);
            protocol.init(network, RSATestUtils.getRandom(network.myId()));
            Serializable myShare = ((BFParameters) param).getMult().shareFromAdditive(shares.get(network.myId()), mod);
            Serializable res = protocol.execute(myShare, set, mod);
            return ((BFParameters) param).getMult().combineToAdditive(res, mod);
        };

        AbstractProtocolTest.ResultCheck<BigInteger> checker = (res) -> {
            BigInteger finalValue = BigInteger.ZERO;
            for (Future<BigInteger> cur : res) {
                finalValue = finalValue.add(cur.get()).mod(mod);
                assertNotEquals(BigInteger.ONE, cur.get());
            }
            // Ensure the result is 0
            assertEquals(BigInteger.ZERO, finalValue);
        };

        Map<Integer, OurParameters> parameters = RSATestUtils.getOurParameters(PRIME_BITLENGTH, STAT_SEC, DEFAULT_PARTIES);
        Map<Integer, INetwork> networks = RSATestUtils.getNetworks(DEFAULT_PARTIES);
        runProtocolTest(networks, parameters, protocolRunner, checker);
    }

    @ParameterizedTest
    @CsvSource({"2,linear", "3,linear", "5,linear", "2,log", "3,log", "5,log","2,const", "3,const", "5,const"})
    public void negative(int parties, String type) throws Exception {
        BigInteger input = BigInteger.valueOf(42);
        List<BigInteger> set = Arrays.asList(BigInteger.valueOf(12), BigInteger.valueOf(2544), BigInteger.valueOf(4), BigInteger.valueOf(1000));
        Map<Integer, BigInteger> shares = share(input, parties, MODULO, rand);

        AbstractProtocolTest.RunProtocol<BigInteger> protocolRunner = (param, network) -> {
            IMembership protocol = switch (type) {
                case "linear" -> new MembershipLinear((BFParameters) param);
                case "log" -> new MembershipLog((BFParameters) param);
                case "const" -> new MembershipConst((BFParameters) param);
                default -> throw new RuntimeException("unknown type");
            };
            protocol.init(network, RSATestUtils.getRandom(network.myId()));
            Serializable myShare = ((BFParameters) param).getMult().shareFromAdditive(shares.get(network.myId()), MODULO);
            Serializable res = protocol.execute(myShare, set, MODULO);
            return ((BFParameters) param).getMult().combineToAdditive(res, MODULO);
        };

        AbstractProtocolTest.ResultCheck<BigInteger> checker = (res) -> {
            BigInteger finalValue = BigInteger.ZERO;
            for (Future<BigInteger> cur : res) {
                finalValue = finalValue.add(cur.get()).mod(MODULO);
                assertNotEquals(BigInteger.ONE, cur.get());
            }
            // Ensure the result is NOT 0
            assertNotEquals(BigInteger.ZERO, finalValue);
        };

        Map<Integer, OurParameters> parameters = RSATestUtils.getOurParameters(PRIME_BITLENGTH, STAT_SEC, parties);
        Map<Integer, INetwork> networks = RSATestUtils.getNetworks(parties);
        runProtocolTest(networks, parameters, protocolRunner, checker);
    }

    // TODO test for equal roots
    @Test
    void polyTest1() {
        Map<Integer, BFParameters> params = RSATestUtils.getBFParameters(128, 40, 2);
        MembershipConst membership = new MembershipConst(params.get(0));
        BigInteger modulus = BigInteger.valueOf(4091);
        List<BigInteger> roots = Arrays.asList(BigInteger.valueOf(1));
        List<BigInteger> ref = Arrays.asList(BigInteger.valueOf(-1).mod(modulus));
        BigInteger[] res = membership.computePolyConsts(roots, modulus);
        assertArrayEquals(ref.toArray(), res);
    }

    @Test
    void polyTest2() {
        Map<Integer, BFParameters> params = RSATestUtils.getBFParameters(128, 40, 2);
        MembershipConst membership = new MembershipConst(params.get(0));
        BigInteger modulus = BigInteger.valueOf(4091);
        List<BigInteger> roots = Arrays.asList(BigInteger.valueOf(1), BigInteger.valueOf(2));
        List<BigInteger> ref = Arrays.asList(BigInteger.valueOf(2), BigInteger.valueOf(-3).mod(modulus));
        BigInteger[] res = membership.computePolyConsts(roots, modulus);
        assertArrayEquals(ref.toArray(), res);
    }

    @Test
    void polyTest3() {
        Map<Integer, BFParameters> params = RSATestUtils.getBFParameters(128, 40, 2);
        MembershipConst membership = new MembershipConst(params.get(0));
        BigInteger modulus = BigInteger.valueOf(4091);
        List<BigInteger> roots = Arrays.asList(BigInteger.valueOf(1), BigInteger.valueOf(2), BigInteger.valueOf(3));
        List<BigInteger> ref = Arrays.asList(BigInteger.valueOf(-6).mod(modulus), BigInteger.valueOf(11).mod(modulus), BigInteger.valueOf(-6).mod(modulus));
        BigInteger[] res = membership.computePolyConsts(roots, modulus);
        assertArrayEquals(ref.toArray(), res);
    }

    @Test
    void polyTest4() {
        Map<Integer, BFParameters> params = RSATestUtils.getBFParameters(128, 40, 2);
        MembershipConst membership = new MembershipConst(params.get(0));
        BigInteger modulus = BigInteger.valueOf(4091);
        List<BigInteger> roots = Arrays.asList(BigInteger.valueOf(1), BigInteger.valueOf(2), BigInteger.valueOf(3), BigInteger.valueOf(4), BigInteger.valueOf(5), BigInteger.valueOf(6));
        List<BigInteger> ref = Arrays.asList(
                BigInteger.valueOf(720).mod(modulus),
                BigInteger.valueOf(-1764).mod(modulus),
                BigInteger.valueOf(1624).mod(modulus),
                BigInteger.valueOf(-735).mod(modulus),
                BigInteger.valueOf(175).mod(modulus),
                BigInteger.valueOf(-21).mod(modulus));
        BigInteger[] res = membership.computePolyConsts(roots, modulus);
        assertArrayEquals(ref.toArray(), res);
    }

}
