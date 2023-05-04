package anonymous.compiler;

import anonymous.AbstractProtocol;
import anonymous.AbstractProtocolTest;
import anonymous.mult.MultFactory;
import anonymous.network.DummyNetwork;
import anonymous.network.NetworkFactory;
import anonymous.rsa.RSATestUtils;
import anonymous.rsa.our.OurParameters;
import anonymous.rsa.our.OurProtocol;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.lang.reflect.Field;
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

public class CompilerTest extends AbstractProtocolTest {
    @ParameterizedTest
    @CsvSource({"2048", "3072", "4096"})
    void bench(int bitlength) throws Exception {
        System.out.println(bitlength);
        int parties = 3;
        Map<Integer, BigInteger> pShares = RSATestUtils.randomPrime(parties, bitlength/2, rand);
        Map<Integer, BigInteger> qShares = RSATestUtils.randomPrime(parties, bitlength/2, rand);
        BigInteger p = pShares.values().stream().reduce(BigInteger.ZERO, (a, b) -> a.add(b));
        BigInteger q = qShares.values().stream().reduce(BigInteger.ZERO, (a, b) -> a.add(b));
        BigInteger N = p.multiply(q);

        // NOTE that we need copies of both, since the network for mult is stored in the parameters!!! Terrible decision! TODO FIX!
        Map<Integer, OurParameters> brainParameters = RSATestUtils.getOurParameters(bitlength/2, STAT_SEC, parties, false, MultFactory.MultType.REPLICATED);
        Map<Integer, OurParameters> pinkyParameters = RSATestUtils.getOurParameters(bitlength/2, STAT_SEC, parties, false, MultFactory.MultType.REPLICATED);
        CompiledNetworkFactory netFactory = new CompiledNetworkFactory(new NetworkFactory(parties));
        Map<Integer, NetworkPair> networks = netFactory.getNetworks(NetworkFactory.NetworkType.DUMMY);
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        List<Future<BigInteger>> res = new ArrayList<>(parties);
        for (int i = 0; i < parties; i++) {
            int finalI = i;
            res.add(executor.submit(() -> {
                OurProtocol.MembershipProtocol membership = OurProtocol.MembershipProtocol.LOG;
                CompiledProtocolResources resources = new CompiledProtocolResources(COMP_SEC);
                BigInteger temp = BigInteger.ZERO;
                for (int j = 0; j < 15; j++) {
                    CompiledProtocol protocol = new CompiledProtocol(resources, new OurProtocol(brainParameters.get(finalI), membership), new OurProtocol(pinkyParameters.get(finalI), membership));
                    protocol.init(networks.get(finalI), RSATestUtils.getRandom(finalI));
                    BigInteger localRes = protocol.execute(Arrays.asList(pShares.get(finalI), qShares.get(finalI)), Arrays.asList(N)).get(0);
                    // ensure things get run
                    temp = temp.add(localRes);
                }
                long time = 0;
                Field privateField = AbstractProtocol.class.getDeclaredField("network");
                privateField.setAccessible(true);
                ((DummyNetwork) ((PinkyNetwork) privateField.get(pinkyParameters.get(finalI).getMult())).internalNetwork).resetCount();
                long max = 0;
                for (int j = 0; j < 30; j++) {
                    CompiledProtocol protocol = new CompiledProtocol(resources, new OurProtocol(brainParameters.get(finalI), membership), new OurProtocol(pinkyParameters.get(finalI), membership));
                    long start = System.currentTimeMillis();
                    protocol.init(networks.get(finalI), RSATestUtils.getRandom(finalI));
                    BigInteger localRes = protocol.execute(Arrays.asList(pShares.get(finalI), qShares.get(finalI)), Arrays.asList(N)).get(0);
                    long stop = System.currentTimeMillis();
                    // ensure things get run
                    temp = temp.add(localRes);
                    time+= stop-start;
                    if (stop-start > max) {
                        max = stop-start;
                    }
                }
                System.out.println("max " + max);
                System.out.println("time: " + ((double) time/30));
                return BigInteger.valueOf(time) ;
            }));
        }
        executor.shutdown();
        assertTrue(executor.awaitTermination(20000, TimeUnit.SECONDS));

//        //TODO ensure correctness of pinky com! Result should 1!
//        for (Future<BigInteger> cur : res) {
//            assertEquals(BigInteger.ONE, cur.get());
//        }

//        runProtocolTest(brainNets, pinkyNets, parameters, protocolRunner, checker);
//        System.out.println(((MultCounter) parameters.get(0).getMult()).toString());
        System.out.println("Nettime " + (((double) ((DummyNetwork) networks.get(0).getBrainNetwork().internalNetwork).getNetworkTime())
        +((DummyNetwork) networks.get(0).getPinkyNetwork().internalNetwork).getNetworkTime())/30);
        System.out.println("Net bytes " + (((double) ((DummyNetwork) networks.get(0).getBrainNetwork().internalNetwork).getBytesSent()
        + ((DummyNetwork) networks.get(0).getPinkyNetwork().internalNetwork).getBytesSent())/30));
    }
}
