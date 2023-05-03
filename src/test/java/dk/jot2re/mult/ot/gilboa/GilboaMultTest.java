package dk.jot2re.mult.ot.gilboa;

import dk.jot2re.mult.IMult;
import dk.jot2re.mult.MultCounter;
import dk.jot2re.mult.MultFactory;
import dk.jot2re.network.DummyNetwork;
import dk.jot2re.network.NetworkFactory;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static dk.jot2re.DefaultSecParameters.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GilboaMultTest {
    @Test
    void sunshineBounded() throws Exception {
        sunshine(PRIME_BITLENGTH-1, MODULO);
    }

    void sunshine(int bitLength, BigInteger modulo) throws Exception {
        int parties = 2;
        BigInteger[] A = new BigInteger[parties];
        BigInteger[] B = new BigInteger[parties];
        MultFactory factory = new MultFactory(parties);
        Map<Integer, IMult> mults = factory.getMults(MultFactory.MultType.GILBOA, NetworkFactory.NetworkType.DUMMY, true);
        Random rand = new Random(42);
        for (int i = 0; i < parties; i++) {
            A[i] = new BigInteger(bitLength, rand);
            B[i] = new BigInteger(bitLength, rand);
        }
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        Field privateField = MultCounter.class.getDeclaredField("network");
        privateField.setAccessible(true);
        List<Future<BigInteger>> C = new ArrayList<>(parties);
        for (int i = 0; i < parties; i++) {
            int finalI = i;
            C.add(executor.submit(() -> {
                ((DummyNetwork) privateField.get(mults.get(finalI))).resetCount();
                long start = System.currentTimeMillis();
                BigInteger res = mults.get(finalI).mult(A[finalI], B[finalI], modulo, bitLength);
                long stop = System.currentTimeMillis();
                System.out.println("sender " + (stop-start));
                return res;
            }));
        }
        executor.shutdown();
        assertTrue(executor.awaitTermination(20000, TimeUnit.SECONDS));
        System.out.println(((MultCounter) mults.get(0)).toString());

        BigInteger refA = Arrays.stream(A).reduce(BigInteger.ZERO, BigInteger::add);
        BigInteger refB = Arrays.stream(B).reduce(BigInteger.ZERO, BigInteger::add);
        BigInteger refC = BigInteger.ZERO;
        for (Future<BigInteger> cur : C) {
            refC = refC.add(cur.get());
        }
        assertEquals(refA.multiply(refB).mod(modulo), refC.mod(modulo));

        DummyNetwork network = (DummyNetwork) privateField.get(mults.get(0));
        System.out.println("Rounds " + network.getRounds());
        System.out.println("Nettime " + network.getNetworkTime());
        System.out.println("Nettrans " + network.getTransfers());
        System.out.println("Net bytes " + network.getBytesSent());
    }
}
