package anonymous.mult.shamir;

import anonymous.network.DummyNetwork;
import anonymous.network.NetworkFactory;
import anonymous.rsa.RSATestUtils;
import anonymous.AbstractProtocol;
import anonymous.mult.IMult;
import anonymous.mult.MultCounter;
import anonymous.mult.MultFactory;
import anonymous.network.INetwork;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static anonymous.DefaultSecParameters.MODULO_BITLENGTH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ShamirMultTest {
    // todo refactor and consolidate with Gilboa
    @ParameterizedTest
    @ValueSource(ints = {3,5})
    void sunshine(int parties) throws Exception {
        SecureRandom rand = new SecureRandom();
        BigInteger modulo = BigInteger.probablePrime(MODULO_BITLENGTH, rand);
        BigInteger[] A = new BigInteger[parties];
        BigInteger[] B = new BigInteger[parties];
        MultFactory factory = new MultFactory(parties);
        Map<Integer, IMult> mults = factory.getMults(MultFactory.MultType.SHAMIR, NetworkFactory.NetworkType.DUMMY, true);
        Map<Integer, INetwork> networks = RSATestUtils.getNetworks(parties);
        for (int i = 0; i < parties; i++) {
            A[i] = new BigInteger(MODULO_BITLENGTH, rand);
            B[i] = new BigInteger(MODULO_BITLENGTH, rand);
        }
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        List<Future<BigInteger>> C = new ArrayList<>(parties);
        for (int i = 0; i < parties; i++) {
            int finalI = i;
            C.add(executor.submit(() -> {
                mults.get(finalI).init(networks.get(finalI), RSATestUtils.getRandom(finalI));
                BigInteger res = null;
                long start = System.currentTimeMillis();
                for (int j = 0; j < 100; j++) {
                    res = (BigInteger) mults.get(finalI).mult(A[finalI], B[finalI], modulo);
                }
                long stop = System.currentTimeMillis();
                System.out.println("Time: " + (stop-start));
                return res;
            }));
        }
        executor.shutdown();
        assertTrue(executor.awaitTermination(20000, TimeUnit.SECONDS));
        System.out.println(((MultCounter) mults.get(0)).toString());

        BigInteger refA = Arrays.stream(A).reduce(BigInteger.ZERO, BigInteger::add);
        BigInteger refB = Arrays.stream(B).reduce(BigInteger.ZERO, BigInteger::add);
        BigInteger res = BigInteger.ZERO;
        for (Future<BigInteger> cur : C) {
            res = res.add(cur.get()).mod(modulo);
        }
        assertEquals(refA.multiply(refB).mod(modulo), res);

        Field privateField = MultCounter.class.getDeclaredField("network");
        privateField.setAccessible(true);
        DummyNetwork network = (DummyNetwork) privateField.get(mults.get(0));
        System.out.println("Rounds " + network.getRounds());
        System.out.println("Nettime " + network.getNetworkTime());
        System.out.println("Nettrans " + network.getTransfers());
        System.out.println("Net bytes " + network.getBytesSent());
    }

    @ParameterizedTest
    @ValueSource(ints = {3,5,7,9,11})
    void bgwDegreeReduction(int parties) throws Exception {
        Random rand = new Random(42);
        BigInteger modulo = BigInteger.probablePrime(MODULO_BITLENGTH, rand);
        MultFactory factory = new MultFactory(parties);
        Map<Integer, IMult> mults = factory.getMults(MultFactory.MultType.SHAMIR, NetworkFactory.NetworkType.DUMMY);
        Map<Integer, INetwork> networks = RSATestUtils.getNetworks(parties);
        BigInteger aValue = new BigInteger(MODULO_BITLENGTH, rand);
        BigInteger bValue = new BigInteger(MODULO_BITLENGTH, rand);
        ShamirEngine engine = new ShamirEngine(parties, new Random(42));
        Map<Integer, BigInteger> sharesOfA = engine.share(aValue, modulo);
        Map<Integer, BigInteger> sharesOfB = engine.share(bValue, modulo);
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        List<Future<BigInteger>> C = new ArrayList<>(parties);
        for (int i = 0; i < parties; i++) {
            int finalI = i;
            C.add(executor.submit(() -> {
                mults.get(finalI).init(networks.get(finalI), RSATestUtils.getRandom(finalI));
                long start = System.currentTimeMillis();
                BigInteger res = null;
                for (int j = 0; j < 100; j++) {
                    res = ((ShamirMult) mults.get(finalI)).bgwDegreeReduction(sharesOfA.get(finalI).multiply(sharesOfB.get(finalI)).mod(modulo), modulo);
                }
                long stop = System.currentTimeMillis();
                System.out.println("Time: " + (stop-start));
                return res;
            }));
        }
        executor.shutdown();
        assertTrue(executor.awaitTermination(20000, TimeUnit.SECONDS));

        List<BigInteger> resShares = new ArrayList<>();
        for (Future<BigInteger> cur : C) {
            resShares.add(cur.get());
        }
        BigInteger refC = engine.combine(engine.getThreshold(), resShares, modulo);
        assertEquals(aValue.multiply(bValue).mod(modulo), refC.mod(modulo));

        Field privateField = AbstractProtocol.class.getDeclaredField("network");
        privateField.setAccessible(true);
        DummyNetwork network = (DummyNetwork) privateField.get(mults.get(0));
        System.out.println("Rounds " + network.getRounds());
        System.out.println("Nettime " + network.getNetworkTime());
        System.out.println("Nettrans " + network.getTransfers());
        System.out.println("Net bytes " + network.getBytesSent());
    }

    @ParameterizedTest
    @ValueSource(ints = {3,5,7,9})
    void regularDegreeReduction(int parties) throws Exception {
        Random rand = new Random(42);
        BigInteger modulo = BigInteger.probablePrime(MODULO_BITLENGTH, rand);
        MultFactory factory = new MultFactory(parties);
        Map<Integer, IMult> mults = factory.getMults(MultFactory.MultType.SHAMIR, NetworkFactory.NetworkType.DUMMY);
        Map<Integer, INetwork> networks = RSATestUtils.getNetworks(parties);
        BigInteger aValue = new BigInteger(MODULO_BITLENGTH, rand);
        BigInteger bValue = new BigInteger(MODULO_BITLENGTH, rand);
        ShamirEngine engine = new ShamirEngine(parties, new Random(42));
        Map<Integer, BigInteger> sharesOfA = engine.share(aValue, modulo);
        Map<Integer, BigInteger> sharesOfB = engine.share(bValue, modulo);
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        List<Future<BigInteger>> C = new ArrayList<>(parties);
        for (int i = 0; i < parties; i++) {
            int finalI = i;
            C.add(executor.submit(() -> {
                mults.get(finalI).init(networks.get(finalI), RSATestUtils.getRandom(finalI));
                long start = System.currentTimeMillis();
                BigInteger res = null;
                for (int j = 0; j < 100; j++) {
                    res = ((ShamirMult) mults.get(finalI)).degreeReduction(sharesOfA.get(finalI).multiply(sharesOfB.get(finalI)).mod(modulo), modulo);
                }
                long stop = System.currentTimeMillis();
                System.out.println("Time: " + (stop-start));
                return res;
            }));
        }
        executor.shutdown();
        assertTrue(executor.awaitTermination(20000, TimeUnit.SECONDS));

        List<BigInteger> resShares = new ArrayList<>();
        for (Future<BigInteger> cur : C) {
            resShares.add(cur.get());
        }
        BigInteger refC = engine.combine(engine.getThreshold(), resShares, modulo);
        assertEquals(aValue.multiply(bValue).mod(modulo), refC.mod(modulo));

        Field privateField = AbstractProtocol.class.getDeclaredField("network");
        privateField.setAccessible(true);
        DummyNetwork network = (DummyNetwork) privateField.get(mults.get(0));
        System.out.println("Rounds " + network.getRounds());
        System.out.println("Nettime " + network.getNetworkTime());
        System.out.println("Nettrans " + network.getTransfers());
        System.out.println("Net bytes " + network.getBytesSent());
    }

    @ParameterizedTest
    @ValueSource(ints = {3,5})
    void shareInput(int parties) throws Exception {
        Random rand = new Random(42);
        BigInteger modulo = BigInteger.probablePrime(MODULO_BITLENGTH, rand);
        MultFactory factory = new MultFactory(parties);
        Map<Integer, IMult> mults = factory.getMults(MultFactory.MultType.SHAMIR, NetworkFactory.NetworkType.DUMMY, true);
        Map<Integer, INetwork> networks = RSATestUtils.getNetworks(parties);
        BigInteger input = new BigInteger(MODULO_BITLENGTH, rand);
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        List<Future<BigInteger>> C = new ArrayList<>(parties);
        for (int i = 0; i < parties; i++) {
            int finalI = i;
            C.add(executor.submit(() -> {
                mults.get(finalI).init(networks.get(finalI), RSATestUtils.getRandom(finalI));
                long start = System.currentTimeMillis();
                BigInteger res = null;
                for (int j = 0; j < 100; j++) {
                    res = (BigInteger) mults.get(finalI).shareFromAdditive(input, modulo);
                }
                long stop = System.currentTimeMillis();
                System.out.println("Time: " + (stop-start));
                return res;
            }));
        }
        executor.shutdown();
        assertTrue(executor.awaitTermination(20000, TimeUnit.SECONDS));
        System.out.println(((MultCounter) mults.get(0)).toString());
        List<BigInteger> resShares = new ArrayList<>();
        for (Future<BigInteger> cur : C) {
            resShares.add(cur.get());
        }
        ShamirEngine engine = new ShamirEngine(parties, new Random(42));
        BigInteger refC = engine.combine(parties-1, resShares, modulo);
        assertEquals(input.multiply(BigInteger.valueOf(parties)).mod(modulo), refC);

//        Field privateField = AbstractProtocol.class.getDeclaredField("network");
//        privateField.setAccessible(true);
//        DummyNetwork network = (DummyNetwork) privateField.get(mults.get(0));
//        System.out.println("Rounds " + network.getRounds());
//        System.out.println("Nettime " + network.getNetworkTime());
//        System.out.println("Nettrans " + network.getTransfers());
//        System.out.println("Net bytes " + network.getBytesSent());
    }
}
