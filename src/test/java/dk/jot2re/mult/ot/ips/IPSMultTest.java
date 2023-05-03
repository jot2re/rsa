package dk.jot2re.mult.ot.ips;

import dk.jot2re.mult.IMult;
import dk.jot2re.mult.MultCounter;
import dk.jot2re.mult.MultFactory;
import dk.jot2re.network.DummyNetwork;
import dk.jot2re.network.NetworkFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static dk.jot2re.DefaultSecParameters.findMaxPrime;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class IPSMultTest {
    // todo refactor and consolidate with Gilboa

    @ParameterizedTest
    @CsvSource({"2,1032", "3,1032", "5,1032"})
    void sunshine(int parties, int bitlength) throws Exception {
        Random rand = new Random(42);
        BigInteger modulo = findMaxPrime(bitlength);
        BigInteger[] A = new BigInteger[parties];
        BigInteger[] B = new BigInteger[parties];
        MultFactory factory = new MultFactory(parties);
        Map<Integer, IMult> mults = factory.getMults(MultFactory.MultType.IPS, NetworkFactory.NetworkType.DUMMY, true);
        for (int i = 0; i < parties; i++) {
            A[i] = new BigInteger(bitlength-2, rand);
            B[i] = new BigInteger(bitlength-2, rand);
        }
        Field privateField = MultCounter.class.getDeclaredField("network");
        privateField.setAccessible(true);
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        List<Future<BigInteger>> C = new ArrayList<>(parties);
        for (int i = 0; i < parties; i++) {
            int finalI = i;
            C.add(executor.submit(() -> {
                BigInteger res = null;
                // Warmup
                for (int j = 0;j < 10; j++) {
                    res = mults.get(finalI).mult(A[finalI], B[finalI], modulo);
                }
                ((DummyNetwork) privateField.get(mults.get(finalI))).resetCount();
                // TODO standard deviation
                long start = System.currentTimeMillis();
                for (int j = 0;j < 100; j++) {
                    res = mults.get(finalI).mult(A[finalI], B[finalI], modulo);
                }
                long stop = System.currentTimeMillis();
                System.out.println("sender " + (stop - start));
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
