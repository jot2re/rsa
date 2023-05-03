package anonymous.compiler;

import anonymous.AbstractProtocolTest;
import anonymous.network.INetwork;
import anonymous.network.NetworkFactory;
import anonymous.rsa.RSATestUtils;
import anonymous.rsa.our.OurParameters;
import anonymous.rsa.our.RSAUtil;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.Future;

import static anonymous.DefaultSecParameters.STAT_SEC;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PostprocessingTest extends AbstractProtocolTest {

    @Test
    public void sunshine() throws Exception {
        int bitlength = 1024;
        int parties = 3;
        BigInteger modulo = BigInteger.TWO.pow(1024);
        BigInteger a = RSAUtil.sample(rand, modulo);
        BigInteger b = RSAUtil.sample(rand, modulo);
        BigInteger c = a.multiply(b).mod(modulo);
        Map<Integer, BigInteger> aShares = RSATestUtils.share(a, parties, modulo, rand);
        Map<Integer, BigInteger> bShares = RSATestUtils.share(b, parties, modulo, rand);
        Map<Integer, BigInteger> cShares = RSATestUtils.share(c, parties, modulo, rand);

        AbstractProtocolTest.RunProtocol<Boolean> protocolRunner = (param, network) -> {
            Postprocessing protocol = new Postprocessing((OurParameters) param);
            protocol.init(network, RSATestUtils.getRandom(network.myId()));
            Serializable myAShare = ((OurParameters) param).getMult().shareFromAdditive(aShares.get(network.myId()), modulo);
            Serializable myBShare = ((OurParameters) param).getMult().shareFromAdditive(bShares.get(network.myId()), modulo);
            Serializable myCShare = ((OurParameters) param).getMult().shareFromAdditive(cShares.get(network.myId()), modulo);
            long start = System.currentTimeMillis();
            boolean res = protocol.execute(Arrays.asList(new Postprocessing.Multiplication(myAShare, myBShare, myCShare, modulo)));
            long stop = System.currentTimeMillis();
            System.out.println("time: " + (stop-start));
            return res;
        };

        AbstractProtocolTest.ResultCheck<Boolean> checker = (res) -> {
            for (Future<Boolean> cur : res) {
                assertTrue(cur.get());
            }
        };

        Map<Integer, OurParameters> parameters = RSATestUtils.getOurParameters(bitlength, STAT_SEC, parties);
        NetworkFactory netFactory = new NetworkFactory(parties);
        Map<Integer, INetwork> nets = netFactory.getNetworks(NetworkFactory.NetworkType.DUMMY);
        runProtocolTest(nets, parameters, protocolRunner, checker);
//        System.out.println(((MultCounter) parameters.get(0).getMult()).toString());
//            long sent = (((DummyNetwork) nets.get(0)).getBytesSent()-((DummyMult) parameters.get(0).getMult()).bytesSend());
////        System.out.println("Net sent " + sent);
//            long received = (((DummyNetwork) nets.get(0)).getBytesReceived()-((DummyMult) parameters.get(0).getMult()).bytesReceived());
////        System.out.println("Net rec " + received);
//            System.out.println("" + parties + ", " + bitlength + ", " + (received+sent)/2);
//            System.out.println("Rounds " + ((DummyNetwork) nets.get(0)).getRounds());
    }
}
