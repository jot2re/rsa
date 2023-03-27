package dk.jot2re.mult;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DummyMultTest {
    private static final int DEFAULT_BIT_LENGTH = 1024;
    @ParameterizedTest
    @ValueSource(ints = { 2, 3, 5})
    public void sunshine(int parties) throws Exception {
        BigInteger modulo = BigInteger.TWO.pow(DEFAULT_BIT_LENGTH);
        BigInteger[] A = new BigInteger[parties];
        BigInteger[] B = new BigInteger[parties];
        DummyMultFactory factory = new DummyMultFactory(parties);
        Map<Integer, IMult> mults = factory.getMults();
        Random rand = new Random(42);
        for (int i = 0; i < parties; i++) {
            A[i] = new BigInteger(DEFAULT_BIT_LENGTH, rand);
            B[i] = new BigInteger(DEFAULT_BIT_LENGTH, rand);
        }
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        List<Future<BigInteger>> C = new ArrayList<>(parties);
        for (int i = 0; i < parties; i++) {
            int finalI = i;
            C.add(executor.submit(() -> mults.get(finalI).mult(A[finalI], B[finalI], modulo)));
        }
        executor.shutdown();
        assertTrue(executor.awaitTermination(3, TimeUnit.SECONDS));

        BigInteger refA = Arrays.stream(A).reduce(BigInteger.ZERO, BigInteger::add);
        BigInteger refB = Arrays.stream(B).reduce(BigInteger.ZERO, BigInteger::add);
        BigInteger refC = BigInteger.ZERO;
        for (Future<BigInteger> cur : C) {
            refC = refC.add(cur.get());
        }
        assertEquals(refC.mod(modulo), refA.multiply(refB).mod(modulo));
        for (int i = 0; i < parties; i++) {
            assertEquals(1, ((DummyMult) mults.get(i)).getMultCalls());
        }
    }
}
