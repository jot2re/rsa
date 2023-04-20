package dk.jot2re.mult.shamir;

import dk.jot2re.mult.IMult;
import dk.jot2re.mult.MultCounter;
import dk.jot2re.mult.MultFactory;
import dk.jot2re.mult.ot.helper.HelperForTests;
import dk.jot2re.mult.ot.util.AesCtrDrbg;
import dk.jot2re.mult.ot.util.DrngImpl;
import dk.jot2re.network.DummyNetwork;
import dk.jot2re.network.NetworkFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static dk.jot2re.DefaultSecParameters.MODULO_BITLENGTH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ShamirMultTest {
    // todo refactor and consolidate with Gilboa
    @ParameterizedTest
    @ValueSource(ints = {3,5})
    void sunshine(int parties) throws Exception {
        Random rand = new Random(42);
        BigInteger modulo = BigInteger.probablePrime(MODULO_BITLENGTH, rand);
        BigInteger[] A = new BigInteger[parties];
        BigInteger[] B = new BigInteger[parties];
        MultFactory factory = new MultFactory(parties);
        Map<Integer, IMult> mults = factory.getMults(MultFactory.MultType.SHAMIR, NetworkFactory.NetworkType.DUMMY, true);
        for (int i = 0; i < parties; i++) {
            A[i] = new BigInteger(MODULO_BITLENGTH, rand);
            B[i] = new BigInteger(MODULO_BITLENGTH, rand);
        }
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        List<Future<BigInteger>> C = new ArrayList<>(parties);
        for (int i = 0; i < parties; i++) {
            int finalI = i;
            C.add(executor.submit(() -> {
                long start = System.currentTimeMillis();
                BigInteger res = null;
                for (int j = 0; j < 100; j++) {
                    res = mults.get(finalI).mult(A[finalI], B[finalI], modulo);
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
    @ValueSource(ints = {3,5})
    void degreeReduction(int parties) throws Exception {
        Random rand = new Random(42);
        BigInteger modulo = BigInteger.probablePrime(MODULO_BITLENGTH, rand);
        MultFactory factory = new MultFactory(parties);
        Map<Integer, IMult> mults = factory.getMults(MultFactory.MultType.SHAMIR, NetworkFactory.NetworkType.DUMMY);
        BigInteger aValue = new BigInteger(MODULO_BITLENGTH, rand);
        BigInteger bValue = new BigInteger(MODULO_BITLENGTH, rand);
        ShamirEngine engine = new ShamirEngine(parties,  new DrngImpl(new AesCtrDrbg(HelperForTests.seedOne)));
        Map<Integer, BigInteger> sharesOfA = engine.share(aValue, modulo);
        Map<Integer, BigInteger> sharesOfB = engine.share(bValue, modulo);
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        List<Future<BigInteger>> C = new ArrayList<>(parties);
        for (int i = 0; i < parties; i++) {
            int finalI = i;
            C.add(executor.submit(() -> {
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

        Field privateField = ShamirMult.class.getDeclaredField("network");
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
        Map<Integer, IMult> mults = factory.getMults(MultFactory.MultType.SHAMIR, NetworkFactory.NetworkType.DUMMY);
        BigInteger input = new BigInteger(MODULO_BITLENGTH, rand);
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        List<Future<BigInteger>> C = new ArrayList<>(parties);
        for (int i = 0; i < parties; i++) {
            int finalI = i;
            C.add(executor.submit(() -> {
                long start = System.currentTimeMillis();
                BigInteger res = null;
                for (int j = 0; j < 100; j++) {
                    res = ((ShamirMult) mults.get(finalI)).shareFromAdditive(input, modulo);
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
        ShamirEngine engine = new ShamirEngine(parties,  new DrngImpl(new AesCtrDrbg(HelperForTests.seedOne)));
        BigInteger refC = engine.combine(parties-1, resShares, modulo);
        assertEquals(input.multiply(BigInteger.valueOf(parties)).mod(modulo), refC);

        Field privateField = ShamirMult.class.getDeclaredField("network");
        privateField.setAccessible(true);
        DummyNetwork network = (DummyNetwork) privateField.get(mults.get(0));
        System.out.println("Rounds " + network.getRounds());
        System.out.println("Nettime " + network.getNetworkTime());
        System.out.println("Nettrans " + network.getTransfers());
        System.out.println("Net bytes " + network.getBytesSent());
    }
}
