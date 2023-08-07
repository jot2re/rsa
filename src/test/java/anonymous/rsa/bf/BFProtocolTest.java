package anonymous.rsa.bf;

import anonymous.AbstractProtocolTest;
import anonymous.rsa.RSATestUtils;
import anonymous.mult.DummyMult;
import anonymous.network.DummyNetwork;
import anonymous.network.INetwork;
import org.dfdeshom.math.GMP;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigInteger;
import java.util.Map;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BFProtocolTest extends AbstractProtocolTest {
    // TODO negative tests
    @ParameterizedTest
    @CsvSource({"2,1024,40", "2,1024,60", "2,1024,80", "2,1024,100", "3,1024,40", "3,1024,60", "3,1024,80", "3,1024,100", "5,1024,40", "5,1024,60", "5,1024,80", "5,1024,100",
            "7,1024,40", "9,1024,40",
            "2,1536,40", "2,1536,60", "2,1536,80", "2,1536,100", "3,1536,40", "3,1536,60", "3,1536,80", "3,1536,100", "5,1536,40", "5,1536,60", "5,1536,80", "5,1536,100",
            "2,2048,40", "2,2048,60", "2,2048,80", "2,2048,100", "3,2048,40", "3,2048,60", "3,2048,80", "3,2048,100", "5,2048,40", "5,2048,60", "5,2048,80", "5,2048,100"})
//    @CsvSource({"2,1024,40", "3,1536,60", "5,2048,80"})
    public void sunshine(int parties, int bitlength, int statSec) throws Exception {
//        int bitlength = 2048;
        Map<Integer, BigInteger> pShares = RSATestUtils.randomPrime(parties, bitlength, rand);
        Map<Integer, BigInteger> qShares = RSATestUtils.randomPrime(parties, bitlength, rand);
        BigInteger p = pShares.values().stream().reduce(BigInteger.ZERO, (a, b)-> a.add(b));
        BigInteger q = qShares.values().stream().reduce(BigInteger.ZERO, (a, b)-> a.add(b));
        BigInteger N = p.multiply(q);

        RunProtocol<Boolean> protocolRunner = (param, network) -> {
            BFProtocol protocol = new BFProtocol((BFParameters) param);
            protocol.init(network, RSATestUtils.getRandom(network.myId()));
            if (!protocol.validateParameters(pShares.get(network.myId()), qShares.get(network.myId()), N)) {
                return false;
            }
            long start = System.currentTimeMillis();
            boolean res = protocol.execute(pShares.get(network.myId()), qShares.get(network.myId()), N);
            long stop = System.currentTimeMillis();
//            System.out.println("time: " + (stop-start));
            return res;
        };

        ResultCheck<Boolean> checker = (res) -> {
            for (Future<Boolean> cur : res) {
                assertTrue(cur.get());
            }
        };

        Map<Integer, BFParameters> parameters = RSATestUtils.getBFParameters(bitlength, statSec, parties, false, true);
        Map<Integer, INetwork> networks = RSATestUtils.getNetworks(parties);
        runProtocolTest(networks, parameters, protocolRunner, checker);
//        System.out.println("Mult calls " + ((DummyMult) parameters.get(0).getMult()).getMultCalls());
//        System.out.println("Rounds " + ((DummyNetwork) networks.get(0)).getRounds());
//        System.out.println("Nettime " + ((DummyNetwork) networks.get(0)).getNetworkTime());
//        System.out.println("Nettrans " + ((DummyNetwork) networks.get(0)).getTransfers());
        long sent = (((DummyNetwork) networks.get(0)).getBytesSent()-((DummyMult) parameters.get(0).getMult()).bytesSend());
//        System.out.println("Net sent " + sent);
        long received = (((DummyNetwork) networks.get(0)).getBytesReceived()-((DummyMult) parameters.get(0).getMult()).bytesReceived());
//        System.out.println("Net rec " + received);
        System.out.println("" + parties + ", " + bitlength + ", " + statSec + ", " + (received+sent)/2);
//        System.out.println("Net avr snd/rec " + (received+sent)/2);
    }

    @ParameterizedTest
    @CsvSource({"2,1024,40", "3,1536,60", "5,2048,80", "7,2048,40", "9,2048,40"})
    public void sunshineWOJNI(int parties, int bitlength, int statSec) throws Exception {
        Map<Integer, BigInteger> pShares = RSATestUtils.randomPrime(parties, bitlength, rand);
        Map<Integer, BigInteger> qShares = RSATestUtils.randomPrime(parties, bitlength, rand);
        BigInteger p = pShares.values().stream().reduce(BigInteger.ZERO, (a, b)-> a.add(b));
        BigInteger q = qShares.values().stream().reduce(BigInteger.ZERO, (a, b)-> a.add(b));
        BigInteger N = p.multiply(q);

        RunProtocol<Boolean> protocolRunner = (param, network) -> {
            BFProtocol protocol = new BFProtocol((BFParameters) param);
            protocol.init(network, RSATestUtils.getRandom(network.myId()));
            if (!protocol.validateParameters(pShares.get(network.myId()), qShares.get(network.myId()), N)) {
                return false;
            }
            long start = System.currentTimeMillis();
            boolean res = protocol.execute(pShares.get(network.myId()), qShares.get(network.myId()), N);
            long stop = System.currentTimeMillis();
//            System.out.println("time: " + (stop-start));
            return res;
        };

        ResultCheck<Boolean> checker = (res) -> {
            for (Future<Boolean> cur : res) {
                assertTrue(cur.get());
            }
        };

        Map<Integer, BFParameters> parameters = RSATestUtils.getBFParameters(bitlength, statSec, parties, false, false);
        Map<Integer, INetwork> networks = RSATestUtils.getNetworks(parties);
        runProtocolTest(networks, parameters, protocolRunner, checker);
//        System.out.println("Mult calls " + ((DummyMult) parameters.get(0).getMult()).getMultCalls());
//        System.out.println("Rounds " + ((DummyNetwork) networks.get(0)).getRounds());
//        System.out.println("Nettime " + ((DummyNetwork) networks.get(0)).getNetworkTime());
//        System.out.println("Nettrans " + ((DummyNetwork) networks.get(0)).getTransfers());
        long sent = (((DummyNetwork) networks.get(0)).getBytesSent()-((DummyMult) parameters.get(0).getMult()).bytesSend());
//        System.out.println("Net sent " + sent);
        long received = (((DummyNetwork) networks.get(0)).getBytesReceived()-((DummyMult) parameters.get(0).getMult()).bytesReceived());
//        System.out.println("Net rec " + received);
        System.out.println("" + parties + ", " + bitlength + ", " + statSec + ", " + (received+sent)/2);
//        System.out.println("Net avr snd/rec " + (received+sent)/2);
    }

    // First 3 tests are from https://www.mathworks.com/help/symbolic/jacobisymbol.html
    @ParameterizedTest
    @CsvSource({"1,9,1", "28,9,1", "14,561,1", "1353,566480805,0", "1353,566480807,1","7,23,-1"})
    public void jacobiTest(int a, int n, int res) {
        assertEquals(res, BFProtocol.jacobiSymbol(BigInteger.valueOf(a), BigInteger.valueOf(n)));
    }

    @Test
    public void jniTestMult() {
        GMP a = new GMP("42");
        GMP b = new GMP("1337");
        b.multiply(a, b);
        BigInteger gmpRes = new BigInteger(b.toString());
        assertEquals(BigInteger.valueOf(42*1337), gmpRes);
    }

    @Test
    public void jniTestPowMod() {
        GMP base = new GMP("2");
        GMP exp = new GMP("8");
        GMP mod = new GMP(251);
        GMP res = new GMP();
        base.modPow(exp, mod, res);
        BigInteger gmpRes = new BigInteger(res.toString());
        // 2^8 mod 251 = 5
        assertEquals(BigInteger.valueOf(5), gmpRes);
    }
}
