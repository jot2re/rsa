package dk.jot2re.compiler;

import dk.jot2re.AbstractProtocolTest;
import dk.jot2re.network.NetworkFactory;
import dk.jot2re.rsa.RSATestUtils;
import dk.jot2re.rsa.our.OurParameters;
import dk.jot2re.rsa.our.OurProtocol;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static dk.jot2re.DefaultSecParameters.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CompilerTest extends AbstractProtocolTest {
    @Test
    public void sunshine() throws Exception {
        int parties = 3;
        Map<Integer, BigInteger> pShares = RSATestUtils.randomPrime(parties, PRIME_BITLENGTH, rand);
        Map<Integer, BigInteger> qShares = RSATestUtils.randomPrime(parties, PRIME_BITLENGTH, rand);
        BigInteger p = pShares.values().stream().reduce(BigInteger.ZERO, (a, b) -> a.add(b));
        BigInteger q = qShares.values().stream().reduce(BigInteger.ZERO, (a, b) -> a.add(b));
        BigInteger N = p.multiply(q);

        // NOTE that we need copies of both, since the network for mult is stored in the parameters!!! Terrible decision! TODO FIX!
        Map<Integer, OurParameters> brainParameters = RSATestUtils.getOurParameters(PRIME_BITLENGTH, STAT_SEC, parties);
        Map<Integer, OurParameters> pinkyParameters = RSATestUtils.getOurParameters(PRIME_BITLENGTH, STAT_SEC, parties);
        CompiledNetworkFactory netFactory = new CompiledNetworkFactory(new NetworkFactory(parties));
        Map<Integer, NetworkPair> networks = netFactory.getNetworks(NetworkFactory.NetworkType.DUMMY);
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        List<Future<BigInteger>> res = new ArrayList<>(parties);
        for (int i = 0; i < parties; i++) {
            int finalI = i;
            res.add(executor.submit(() -> {
                OurProtocol.MembershipProtocol membership = OurProtocol.MembershipProtocol.LINEAR;
                CompiledProtocolResources resources = new CompiledProtocolResources(COMP_SEC);
                CompiledProtocol protocol = new CompiledProtocol(resources, new OurProtocol(brainParameters.get(finalI), membership), new OurProtocol(pinkyParameters.get(finalI), membership));
                long start = System.currentTimeMillis();
                protocol.init(networks.get(finalI), RSATestUtils.getRandom(finalI));
                BigInteger localRes = protocol.execute(Arrays.asList(pShares.get(finalI), qShares.get(finalI)), Arrays.asList(N)).get(0);
                long stop = System.currentTimeMillis();
                System.out.println("time: " + (stop-start));
                return localRes;
            }));
        }
        executor.shutdown();
        assertTrue(executor.awaitTermination(20000, TimeUnit.SECONDS));

        for (Future<BigInteger> cur : res) {
            assertEquals(BigInteger.ONE, cur.get());
        }


//        runProtocolTest(brainNets, pinkyNets, parameters, protocolRunner, checker);
//        System.out.println(((MultCounter) parameters.get(0).getMult()).toString());
//        System.out.println("Rounds " + ((DummyNetwork) brainNets.get(0)).getRounds());
//        System.out.println("Nettime " + ((DummyNetwork) nets.get(0)).getNetworkTime());
//        System.out.println("Nettrans " + ((DummyNetwork) nets.get(0)).getTransfers());
//        System.out.println("Net bytes " + ((DummyNetwork) nets.get(0)).getBytesSent());
    }
}
