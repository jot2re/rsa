package anonymous.compiler;

import anonymous.AbstractProtocolTest;
import anonymous.mult.MultFactory;
import anonymous.network.NetworkFactory;
import anonymous.rsa.RSATestUtils;
import anonymous.rsa.our.OurParameters;
import anonymous.rsa.our.RSAUtil;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static anonymous.DefaultSecParameters.COMP_SEC;
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

        Map<Integer, OurParameters> sharingParams = RSATestUtils.getOurParameters(bitlength, STAT_SEC, parties, false, MultFactory.MultType.REPLICATED);
        Map<Integer, OurParameters> brainParameters = RSATestUtils.getOurParameters(bitlength, STAT_SEC, parties, false, MultFactory.MultType.REPLICATED);
        Map<Integer, OurParameters> pinkyParameters = RSATestUtils.getOurParameters(bitlength, STAT_SEC, parties, false, MultFactory.MultType.REPLICATED);
        CompiledNetworkFactory netFactory = new CompiledNetworkFactory(new NetworkFactory(parties));
        Map<Integer, NetworkPair> networks = netFactory.getNetworks(NetworkFactory.NetworkType.DUMMY);
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        List<Future<Boolean>> res = new ArrayList<>(parties);
        for (int i = 0; i < parties; i++) {
            int finalI = i;
            res.add(executor.submit(() -> {
                PostprocessingProtocol protocol = new PostprocessingProtocol(brainParameters.get(finalI), pinkyParameters.get(finalI), COMP_SEC);
                protocol.init(networks.get(finalI), RSATestUtils.getRandom(networks.get(finalI).getBrainNetwork().myId()));
                sharingParams.get(finalI).getMult().init(networks.get(finalI).getBrainNetwork().internalNetwork, RSATestUtils.getRandom(networks.get(finalI).getBrainNetwork().myId()));
                Serializable myAShare = sharingParams.get(finalI).getMult().shareFromAdditive(aShares.get(networks.get(finalI).getBrainNetwork().myId()), modulo);
                Serializable myBShare = sharingParams.get(finalI).getMult().shareFromAdditive(bShares.get(networks.get(finalI).getBrainNetwork().myId()), modulo);
                Serializable myCShare = sharingParams.get(finalI).getMult().shareFromAdditive(cShares.get(networks.get(finalI).getBrainNetwork().myId()), modulo);
                long start = System.currentTimeMillis();
                boolean curRes = protocol.execute(Arrays.asList(new PostprocessingProtocol.Multiplication(myAShare, myBShare, myCShare, modulo)));
                long stop = System.currentTimeMillis();
                System.out.println("time: " + (stop-start));
                return curRes;
            }));
        }
        executor.shutdown();
        assertTrue(executor.awaitTermination(20000, TimeUnit.SECONDS));

        for (Future<Boolean> cur : res) {
            assertTrue(cur.get());
        }

//        System.out.println(((MultCounter) brainParameters.get(0).getMult()).toString());
//            long sent = (((DummyNetwork) nets.get(0)).getBytesSent()-((DummyMult) parameters.get(0).getMult()).bytesSend());
////        System.out.println("Net sent " + sent);
//            long received = (((DummyNetwork) nets.get(0)).getBytesReceived()-((DummyMult) parameters.get(0).getMult()).bytesReceived());
////        System.out.println("Net rec " + received);
//            System.out.println("" + parties + ", " + bitlength + ", " + (received+sent)/2);
//            System.out.println("Rounds " + ((DummyNetwork) nets.get(0)).getRounds());
    }
}
