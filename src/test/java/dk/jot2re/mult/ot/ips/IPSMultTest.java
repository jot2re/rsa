package dk.jot2re.mult.ot.ips;

import dk.jot2re.mult.IMult;
import dk.jot2re.mult.ot.MultResourcePool;
import dk.jot2re.mult.ot.ot.base.DummyOt;
import dk.jot2re.mult.ot.ot.base.Ot;
import dk.jot2re.mult.ot.util.AesCtrDrbg;
import dk.jot2re.mult.ot.util.Drbg;
import dk.jot2re.network.DummyNetworkFactory;
import dk.jot2re.network.INetwork;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class IPSMultTest {
    // todo refactor and consolidate with Gilboa
    private static final int COMP_SEC = 128;
    private static final int STAT_SEC = 40;
    private static final int DEFAULT_BIT_LENGTH = 2048;// MUST be two-power

    public static Map<Integer, IMult> getMults(int parties, int comp_sec, int statSec, boolean safeExpansion) throws ExecutionException, InterruptedException {
        // todo generalize to more than 2
        DummyNetworkFactory netFactory = new DummyNetworkFactory(parties);
        Map<Integer, INetwork> networks = netFactory.getNetworks();
        Map<Integer, Future<IMult>> mults = new HashMap<>(parties);

        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        for (int i = 0; i < parties; i++) {
            int finalI = i;
            Future<IMult> curMult = executor.submit(() -> {
                Map<Integer, Ot> ots = new HashMap<>(parties-1);
                for (int j = 0; j < parties; j++) {
                    if (finalI != j) {
                        ots.put(j, new DummyOt(j, networks.get(finalI)));
                    }
                }
                byte[] seed = new byte[32];
                seed[0] = (byte) finalI;
                Drbg rand = new AesCtrDrbg(seed);
                MultResourcePool pool = new MultResourcePool(ots, finalI, comp_sec, statSec, networks.get(finalI), rand);
                // the constant mult be a 2-power larger than the amount of parties
                IMult mult = new IPSMult(pool, 32 * DEFAULT_BIT_LENGTH, safeExpansion);
                mult.init(networks.get(finalI));
                return mult;
            });
            mults.put(i, curMult);
        }
        executor.shutdown();
        assertTrue(executor.awaitTermination(30000, TimeUnit.SECONDS));
        Map<Integer, IMult> res = new HashMap<>(parties);
        for (int i: mults.keySet()) {
            res.put(i, mults.get(i).get());
        }
        return res;
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 3, 5})
    void sunshine(int parties) throws Exception {
        BigInteger modulo = BigInteger.TWO.pow(DEFAULT_BIT_LENGTH).subtract(BigInteger.ONE);
        BigInteger[] A = new BigInteger[parties];
        BigInteger[] B = new BigInteger[parties];
        Map<Integer, IMult> mults = getMults(parties, COMP_SEC, STAT_SEC, false);
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
