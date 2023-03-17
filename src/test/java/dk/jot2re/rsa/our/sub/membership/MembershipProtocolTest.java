package dk.jot2re.rsa.our.sub.membership;

import dk.jot2re.AbstractProtocolTest;
import dk.jot2re.rsa.bf.BFParameters;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import static dk.jot2re.rsa.RSATestUtils.share;
import static org.junit.jupiter.api.Assertions.*;

public class MembershipProtocolTest extends AbstractProtocolTest {

    @ParameterizedTest
    @CsvSource({"2,const"})// "3,linear", "5,linear", "2,log", "3,log", "5,log"})
    public void sunshine(int parties, String type) throws Exception {
        BigInteger mod = BigInteger.valueOf(137);
        BigInteger input = BigInteger.valueOf(2);
        List<BigInteger> set = Arrays.asList(BigInteger.valueOf(1), BigInteger.valueOf(2), BigInteger.valueOf(3));
        Map<Integer, BigInteger> shares = share(input, parties, mod, rand);

        AbstractProtocolTest.RunProtocol<BigInteger> protocolRunner = (param) -> {
            IMembership protocol;
            if (type.equals("linear")) {
                protocol = new MembershipLinear((BFParameters) param);
            } else if (type.equals("log")) {
                protocol = new MembershipLog((BFParameters) param);
            } else if (type.equals("const")) {
                protocol = new MembershipConst((BFParameters) param);
            } else {
                throw new RuntimeException("unknown type");
            }
            return protocol.execute(shares.get(param.getMyId()), set, mod);
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

        runProtocolTest(DEFAULT_BIT_LENGTH, DEFAULT_STAT_SEC, parties, protocolRunner, checker);
    }

    @ParameterizedTest
    @CsvSource({"2,linear", "3,linear", "5,linear", "2,log", "3,log", "5,log"})
    public void negative(int parties, String type) throws Exception {
        BigInteger input = BigInteger.valueOf(42);
        List<BigInteger> set = Arrays.asList(BigInteger.valueOf(12), BigInteger.valueOf(2544), BigInteger.valueOf(4), BigInteger.valueOf(1000));
        Map<Integer, BigInteger> shares = share(input, parties, modulo, rand);

        AbstractProtocolTest.RunProtocol<BigInteger> protocolRunner = (param) -> {
            IMembership protocol;
            if (type.equals("linear")) {
                protocol = new MembershipLinear((BFParameters) param);
            } else if (type.equals("log")) {
                protocol = new MembershipLog((BFParameters) param);
            } else if (type.equals("const")) {
                protocol = new MembershipConst((BFParameters) param);
            } else {
                throw new RuntimeException("unknown type");
            }
            return protocol.execute(shares.get(param.getMyId()), set, modulo);
        };

        AbstractProtocolTest.ResultCheck<BigInteger> checker = (res) -> {
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

    // TODO test for equal roots
    @Test
    void polyTest1() {
        MembershipConst membership = new MembershipConst(params.get(0));
        BigInteger modulus = BigInteger.valueOf(4091);
        List<BigInteger> roots = Arrays.asList(BigInteger.valueOf(1));
        List<BigInteger> ref = Arrays.asList(BigInteger.valueOf(-1).mod(modulus));
        BigInteger[] res = membership.computePolyConsts(roots, modulus);
        assertArrayEquals(ref.toArray(), res);
    }

    @Test
    void polyTest2() {
        MembershipConst membership = new MembershipConst(params.get(0));
        BigInteger modulus = BigInteger.valueOf(4091);
        List<BigInteger> roots = Arrays.asList(BigInteger.valueOf(1), BigInteger.valueOf(2));
        List<BigInteger> ref = Arrays.asList(BigInteger.valueOf(2), BigInteger.valueOf(-3).mod(modulus));
        BigInteger[] res = membership.computePolyConsts(roots, modulus);
        assertArrayEquals(ref.toArray(), res);
    }

    @Test
    void polyTest3() {
        MembershipConst membership = new MembershipConst(params.get(0));
        BigInteger modulus = BigInteger.valueOf(4091);
        List<BigInteger> roots = Arrays.asList(BigInteger.valueOf(1), BigInteger.valueOf(2), BigInteger.valueOf(3));
        List<BigInteger> ref = Arrays.asList(BigInteger.valueOf(-6).mod(modulus), BigInteger.valueOf(11).mod(modulus), BigInteger.valueOf(-6).mod(modulus));
        BigInteger[] res = membership.computePolyConsts(roots, modulus);
        assertArrayEquals(ref.toArray(), res);
    }

    @Test
    void polyTest4() {
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
