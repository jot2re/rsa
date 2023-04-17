package dk.jot2re.mult.shamir;

import dk.jot2re.mult.IMult;
import dk.jot2re.mult.MultFactory;
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
        Map<Integer, IMult> mults = factory.getMults(MultFactory.MultType.SHAMIR, NetworkFactory.NetworkType.DUMMY);
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

        BigInteger refA = Arrays.stream(A).reduce(BigInteger.ZERO, BigInteger::add);
        BigInteger refB = Arrays.stream(B).reduce(BigInteger.ZERO, BigInteger::add);
        List<BigInteger> resShares = new ArrayList<>();
        for (Future<BigInteger> cur : C) {
            resShares.add(cur.get());
        }
        BigInteger refC = ((ShamirMult) mults.get(0)).combine(resShares.size()-1, resShares, modulo);
        assertEquals(refA.multiply(refB).mod(modulo), refC.mod(modulo));

        Field privateField = ShamirMult.class.getDeclaredField("network");
        privateField.setAccessible(true);
        DummyNetwork network = (DummyNetwork) privateField.get(mults.get(0));
        System.out.println("Rounds " + network.getRounds());
        System.out.println("Nettime " + network.getNetworkTime());
        System.out.println("Nettrans " + network.getTransfers());
        System.out.println("Net bytes " + network.getBytesSent());
    }
}
