package dk.jot2re.rsa.our;

import dk.jot2re.AbstractProtocolTest;
import dk.jot2re.mult.MultCounter;
import dk.jot2re.network.DummyNetwork;
import dk.jot2re.rsa.RSATestUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigInteger;
import java.util.Map;
import java.util.concurrent.Future;

import static dk.jot2re.DefaultSecParameters.PRIME_BITLENGTH;
import static dk.jot2re.DefaultSecParameters.STAT_SEC;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OurProtocolTest extends AbstractProtocolTest {
    // TODO negative tests
    @ParameterizedTest
    @CsvSource({"2,linear", "3,linear", "5,linear", "2,log", "3,log", "5,log", "2,const", "3,const", "5,const"})
    public void sunshine(int parties, String type) throws Exception {
        Map<Integer, BigInteger> pShares = RSATestUtils.randomPrime(parties, PRIME_BITLENGTH, rand);
        Map<Integer, BigInteger> qShares = RSATestUtils.randomPrime(parties, PRIME_BITLENGTH, rand);
        BigInteger p = pShares.values().stream().reduce(BigInteger.ZERO, (a, b) -> a.add(b));
        BigInteger q = qShares.values().stream().reduce(BigInteger.ZERO, (a, b) -> a.add(b));
        BigInteger N = p.multiply(q);

        AbstractProtocolTest.RunProtocol<Boolean> protocolRunner = (param) -> {
            OurProtocol.MembershipProtocol membership = switch (type) {
                case "linear" -> OurProtocol.MembershipProtocol.LINEAR;
                case "log" -> OurProtocol.MembershipProtocol.LOG;
                case "const" -> OurProtocol.MembershipProtocol.CONST;
                default -> throw new RuntimeException("unknown type");
            };
            OurProtocol protocol = new OurProtocol((OurParameters) param, membership);
            if (!protocol.validateParameters(pShares.get(param.getMyId()), qShares.get(param.getMyId()), N)) {
                return false;
            }
            long start = System.currentTimeMillis();
            boolean res = protocol.execute(pShares.get(param.getMyId()), qShares.get(param.getMyId()), N);
            long stop = System.currentTimeMillis();
            System.out.println("time: " + (stop-start));
            return res;
        };

        AbstractProtocolTest.ResultCheck<Boolean> checker = (res) -> {
            for (Future<Boolean> cur : res) {
                assertTrue(cur.get());
            }
        };

        Map<Integer, OurParameters> parameters = RSATestUtils.getOurParameters(PRIME_BITLENGTH, STAT_SEC, parties);
        runProtocolTest(parameters, protocolRunner, checker);
        System.out.println(((MultCounter) parameters.get(0).getMult()).toString());
        System.out.println("Rounds " + ((DummyNetwork) parameters.get(0).getNetwork()).getRounds());
        System.out.println("Nettime " + ((DummyNetwork) parameters.get(0).getNetwork()).getNetworkTime());
        System.out.println("Nettrans " + ((DummyNetwork) parameters.get(0).getNetwork()).getTransfers());
        System.out.println("Net bytes " + ((DummyNetwork) parameters.get(0).getNetwork()).getBytesSent());
    }

}
