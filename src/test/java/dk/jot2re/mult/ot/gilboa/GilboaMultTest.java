package dk.jot2re.mult.ot.gilboa;

import dk.jot2re.mult.IMult;
import dk.jot2re.mult.ot.ot.otextension.OtExtensionResourcePool;
import dk.jot2re.mult.ot.ot.otextension.OtExtensionTestContext;
import dk.jot2re.network.DummyNetworkFactory;
import dk.jot2re.network.INetwork;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GilboaMultTest {
    private static final int COMP_SEC = 128;
    private static final int STAT_SEC = 40;
    private static final int DEFAULT_BIT_LENGTH = 2048;// MUST be two-power

    public static Map<Integer, IMult> getMults(int parties, int comp_sec, int statSec, boolean safeExpansion) throws ExecutionException, InterruptedException {
        // todo generalize to more than 2
        DummyNetworkFactory netFactory = new DummyNetworkFactory(parties);
        Map<Integer, INetwork> networks = netFactory.getNetworks();

        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        Future<IMult> multZero = executor.submit(() -> {
            OtExtensionResourcePool pool = (new OtExtensionTestContext(0,1, comp_sec, statSec, networks.get(0))).createResources(0);
            IMult mult = new GilboaMult(pool, 32*DEFAULT_BIT_LENGTH, safeExpansion);
            mult.init(networks.get(0));
            return mult;
        });
        Future<IMult> multOne = executor.submit(() -> {
            OtExtensionResourcePool pool =(new OtExtensionTestContext(1,0, comp_sec, statSec, networks.get(1))).createResources(0);
            IMult mult = new GilboaMult(pool, 32*DEFAULT_BIT_LENGTH, safeExpansion);
            mult.init(networks.get(1));
            return mult;
        });
        executor.shutdown();
        assertTrue(executor.awaitTermination(30000, TimeUnit.SECONDS));
        Map<Integer, IMult> mults = new HashMap<>(parties);
        mults.put(0, multZero.get());
        mults.put(1, multOne.get());
        return mults;
    }

    @Test
    void sunshine() throws Exception {
        int parties = 2;
        BigInteger modulo = BigInteger.TWO.pow(DEFAULT_BIT_LENGTH).subtract(BigInteger.ONE);
        BigInteger[] A = new BigInteger[parties];
        BigInteger[] B = new BigInteger[parties];
        Map<Integer, IMult> mults = getMults(2, COMP_SEC, STAT_SEC, true);
        Random rand = new Random(42);
        for (int i = 0; i < parties; i++) {
            A[i] = new BigInteger(DEFAULT_BIT_LENGTH, rand);
            B[i] = new BigInteger(DEFAULT_BIT_LENGTH, rand);
        }
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        List<Future<BigInteger>> C = new ArrayList<>(parties);
        for (int i = 0; i < parties; i++) {
            int finalI = i;
            C.add(executor.submit(() -> {
                long start = System.currentTimeMillis();
                BigInteger res =mults.get(finalI).mult(A[finalI], B[finalI], modulo);
                long stop = System.currentTimeMillis();
                System.out.println("sender " + (stop-start));
                return res;
            }));
        }
        executor.shutdown();
        assertTrue(executor.awaitTermination(20000, TimeUnit.SECONDS));

        BigInteger refA = Arrays.stream(A).reduce(BigInteger.ZERO, BigInteger::add);
        BigInteger refB = Arrays.stream(B).reduce(BigInteger.ZERO, BigInteger::add);
        BigInteger refC = BigInteger.ZERO;
        for (Future<BigInteger> cur : C) {
            refC = refC.add(cur.get());
        }
        assertEquals(refA.multiply(refB).mod(modulo), refC.mod(modulo));
    }
}