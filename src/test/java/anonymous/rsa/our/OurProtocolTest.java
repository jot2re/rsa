package anonymous.rsa.our;

import anonymous.AbstractProtocolTest;
import anonymous.network.NetworkFactory;
import anonymous.rsa.RSATestUtils;
import anonymous.mult.MultCounter;
import anonymous.network.INetwork;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigInteger;
import java.util.Map;
import java.util.concurrent.Future;

import static anonymous.DefaultSecParameters.STAT_SEC;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OurProtocolTest extends AbstractProtocolTest {
    // TODO negative tests

        @ParameterizedTest
    @CsvSource({"2,1024,linear", "3,1024,linear", "5,1024,linear", "2,1024,log", "3,1024,log", "5,1024,log", "2,1024,const", "3,1024,const", "5,1024,const"})
//    @CsvSource({"2,1024,linear", "3,1024,linear", "5,1024,linear", "2,1536,linear", "3,1536,linear", "5,1536,linear", "2,2048,linear", "3,2048,linear", "5,2048,linear"})
    public void sunshine(int parties, int bitlength, String type) throws Exception {
        Map<Integer, BigInteger> pShares = RSATestUtils.randomPrime(parties, bitlength, rand);
        Map<Integer, BigInteger> qShares = RSATestUtils.randomPrime(parties, bitlength, rand);
        BigInteger p = pShares.values().stream().reduce(BigInteger.ZERO, (a, b) -> a.add(b));
        BigInteger q = qShares.values().stream().reduce(BigInteger.ZERO, (a, b) -> a.add(b));
        BigInteger N = p.multiply(q);

        AbstractProtocolTest.RunProtocol<Boolean> protocolRunner = (param, network) -> {
            OurProtocol.MembershipProtocol membership = switch (type) {
                case "linear" -> OurProtocol.MembershipProtocol.LINEAR;
                case "log" -> OurProtocol.MembershipProtocol.LOG;
                case "const" -> OurProtocol.MembershipProtocol.CONST;
                default -> throw new RuntimeException("unknown type");
            };
            OurProtocol protocol = new OurProtocol((OurParameters) param, membership);
            protocol.init(network, RSATestUtils.getRandom(network.myId()));
            if (!protocol.validateParameters(pShares.get(network.myId()), qShares.get(network.myId()), N)) {
                return false;
            }
            long start = System.currentTimeMillis();
            boolean res = protocol.execute(pShares.get(network.myId()), qShares.get(network.myId()), N);
            long stop = System.currentTimeMillis();
            System.out.println("time: " + (stop-start));
            return res;
        };

        AbstractProtocolTest.ResultCheck<Boolean> checker = (res) -> {
            for (Future<Boolean> cur : res) {
                assertTrue(cur.get());
            }
        };

        Map<Integer, OurParameters> parameters = RSATestUtils.getOurParameters(bitlength, STAT_SEC, parties, true);
        NetworkFactory netFactory = new NetworkFactory(parties);
        Map<Integer, INetwork> nets = netFactory.getNetworks(NetworkFactory.NetworkType.DUMMY);
        runProtocolTest(nets, parameters, protocolRunner, checker);
        System.out.println("" + parties + ", " + type);
        System.out.println(((MultCounter) parameters.get(0).getMult()).toString());
//            long sent = (((DummyNetwork) nets.get(0)).getBytesSent()-((DummyMult) parameters.get(0).getMult()).bytesSend());
////        System.out.println("Net sent " + sent);
//            long received = (((DummyNetwork) nets.get(0)).getBytesReceived()-((DummyMult) parameters.get(0).getMult()).bytesReceived());
////        System.out.println("Net rec " + received);
//            System.out.println("" + parties + ", " + bitlength + ", " + (received+sent)/2);
//            System.out.println("Rounds " + ((DummyNetwork) nets.get(0)).getRounds());
    }

}
