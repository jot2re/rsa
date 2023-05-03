package anonymous.mult;

import anonymous.rsa.RSATestUtils;
import anonymous.network.INetwork;
import anonymous.network.NetworkFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static anonymous.DefaultSecParameters.MODULO;
import static anonymous.DefaultSecParameters.MODULO_BITLENGTH;
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
        Map<Integer, INetwork> nets = RSATestUtils.getNetworks(parties);
        Random rand = new Random(42);
        for (int i = 0; i < parties; i++) {
            A[i] = new BigInteger(MODULO_BITLENGTH, rand);
            B[i] = new BigInteger(MODULO_BITLENGTH, rand);
        }
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        List<Future<BigInteger>> C = new ArrayList<>(parties);
        for (int i = 0; i < parties; i++) {
            int finalI = i;
            C.add(executor.submit(() -> {
                mults.get(finalI).init(nets.get(finalI), RSATestUtils.getRandom(finalI));
                return mults.get(finalI).mult(A[finalI], B[finalI], MODULO);
            }));
        }
        executor.shutdown();
        assertTrue(executor.awaitTermination(3, TimeUnit.SECONDS));

        BigInteger refA = Arrays.stream(A).reduce(BigInteger.ZERO, BigInteger::add);
        BigInteger refB = Arrays.stream(B).reduce(BigInteger.ZERO, BigInteger::add);
        BigInteger refC = BigInteger.ZERO;
        for (Future<BigInteger> cur : C) {
            refC = refC.add(cur.get());
        }
        assertEquals(refC.mod(MODULO), refA.multiply(refB).mod(MODULO));
        for (int i = 0; i < parties; i++) {
            assertEquals(1, ((DummyMult) mults.get(i)).getMultCalls());
        }
    }
}
