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
    private static final int DEFAULT_BIT_LENGTH = 1024;
    private static final int COMP_SEC = 256;
    private static final int STAT_SEC = 40;

    public static Map<Integer, OtExtensionResourcePool> getOtParameters(int parties, int comp_sec, int statSec) {
        try {
            // todo generalize to more than 2
            DummyNetworkFactory netFactory = new DummyNetworkFactory(parties);
            Map<Integer, INetwork> networks = netFactory.getNetworks();
            OtExtensionTestContext contextZero = new OtExtensionTestContext(0,1, comp_sec, statSec, networks.get(0));
            OtExtensionTestContext contextOne = new OtExtensionTestContext(1,0, comp_sec, statSec, networks.get(1));
            Map<Integer, OtExtensionResourcePool> otResources = new HashMap<>(2);
            otResources.put(0, contextZero.createResources(0));
            otResources.put(1, contextOne.createResources(0));
            return otResources;
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public static Map<Integer, IMult> getMults(int parties, int comp_sec, int statSec) {
        Map<Integer, IMult> mults = new HashMap<>(parties);
        Map<Integer, OtExtensionResourcePool> otResources = getOtParameters(parties, comp_sec, statSec);
        for (int i = 0; i < parties; i++) {
            mults.put(i, new GilboaMult(otResources.get(i)));
            mults.get(i).init(otResources.get(i).getNetwork());
        }
        return mults;
    }


    @Test
    void sunshine() throws Exception {
        int parties = 2;
        BigInteger modulo = BigInteger.TWO.pow(DEFAULT_BIT_LENGTH);
        BigInteger[] A = new BigInteger[parties];
        BigInteger[] B = new BigInteger[parties];
        Map<Integer, IMult> mults = getMults(2, COMP_SEC, STAT_SEC);
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
