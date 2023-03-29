package dk.jot2re.mult.gilboa;

import dk.jot2re.mult.DummyMult;
import dk.jot2re.mult.IMult;
import dk.jot2re.mult.gilboa.ot.otextension.OtExtensionResourcePool;
import dk.jot2re.mult.gilboa.ot.otextension.OtExtensionTestContext;
import dk.jot2re.network.DummyNetworkFactory;
import dk.jot2re.network.INetwork;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GilboaMultTest {
    private static final int COMP_SEC = 128;
    private static final int STAT_SEC = 40;
    private static final int DEFAULT_BIT_LENGTH = 2048-COMP_SEC-STAT_SEC;

    public static Map<Integer, OtExtensionResourcePool> getOtParameters(int parties, int comp_sec, int statSec) {
        try {
            // todo generalize to more than 2
            DummyNetworkFactory netFactory = new DummyNetworkFactory(parties);
            Map<Integer, INetwork> networks = netFactory.getNetworks();

            ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
            Future<OtExtensionResourcePool> contextZero = executor.submit(() -> (new OtExtensionTestContext(0,1, comp_sec, statSec, networks.get(0))).createResources(0));
            Future<OtExtensionResourcePool> contextOne = executor.submit(() -> (new OtExtensionTestContext(1,0, comp_sec, statSec, networks.get(1))).createResources(0));
            executor.shutdown();
            assertTrue(executor.awaitTermination(30000, TimeUnit.SECONDS));

            Map<Integer, OtExtensionResourcePool> otResources = new HashMap<>(2);
            otResources.put(0, contextZero.get());
            otResources.put(1, contextOne.get());
            return otResources;
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public static Map<Integer, IMult> getMults(int parties, int comp_sec, int statSec, boolean safeExpansion) {
        Map<Integer, IMult> mults = new HashMap<>(parties);
        Map<Integer, OtExtensionResourcePool> otResources = getOtParameters(parties, comp_sec, statSec);
        for (int i = 0; i < parties; i++) {
            mults.put(i, new GilboaMult(otResources.get(i), safeExpansion));
            mults.get(i).init(otResources.get(i).getNetwork());
        }
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
            C.add(executor.submit(() -> mults.get(finalI).mult(A[finalI], B[finalI], modulo)));
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
