package dk.jot2re.mult;

import dk.jot2re.network.NetworkFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static dk.jot2re.DefaultSecParameters.MODULO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DummyMultTest {
    @ParameterizedTest
    @ValueSource(ints = { 2, 3, 5})
    public void sunshine(int parties) throws Exception {
        BigInteger[] A = new BigInteger[parties];
        BigInteger[] B = new BigInteger[parties];
        MultFactory factory = new MultFactory(parties);
        Map<Integer, IMult> mults = factory.getMults(MultFactory.MultType.DUMMY, NetworkFactory.NetworkType.DUMMY);
        Random rand = new Random(42);
        for (int i = 0; i < parties; i++) {
            A[i] = BigInteger.valueOf(42);// new BigInteger(MODULO_BITLENGTH, rand);
            B[i] = BigInteger.valueOf(42);// new BigInteger(MODULO_BITLENGTH, rand);
        }
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        List<Future<BigInteger>> C = new ArrayList<>(parties);
        for (int i = 0; i < parties; i++) {
            int finalI = i;
            C.add(executor.submit(() -> mults.get(finalI).mult(A[finalI], B[finalI], MODULO)));
        }
        executor.shutdown();
        assertTrue(executor.awaitTermination(3, TimeUnit.SECONDS));

        BigInteger refA = Arrays.stream(A).reduce(BigInteger.ZERO, BigInteger::add);
        BigInteger refB = Arrays.stream(B).reduce(BigInteger.ZERO, BigInteger::add);
        BigInteger refC = BigInteger.ZERO;
        for (Future<BigInteger> cur : C) {
            refC = refC.add(cur.get());
        }
        assertEquals(refA.multiply(refB).mod(MODULO), refC.mod(MODULO));
        for (int i = 0; i < parties; i++) {
            assertEquals(1, ((DummyMult) mults.get(i)).getMultCalls());
        }
    }
}
