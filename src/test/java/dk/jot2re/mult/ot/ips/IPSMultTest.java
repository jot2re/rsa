package dk.jot2re.mult.ot.ips;

import dk.jot2re.mult.IMult;
import dk.jot2re.mult.ot.OTMultResourcePool;
import dk.jot2re.mult.ot.ot.base.DummyOt;
import dk.jot2re.mult.ot.ot.base.Ot;
import dk.jot2re.mult.ot.util.AesCtrDrbg;
import dk.jot2re.mult.ot.util.Drbg;
import dk.jot2re.network.DummyNetwork;
import dk.jot2re.network.NetworkFactory;
import dk.jot2re.network.INetwork;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.*;

import static dk.jot2re.DefaultSecParameters.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class IPSMultTest {
    // todo refactor and consolidate with Gilboa

    public static Map<Integer, IMult> getMults(int parties, int comp_sec, int statSec, boolean safeExpansion) throws ExecutionException, InterruptedException {
        NetworkFactory netFactory = new NetworkFactory(parties);
        Map<Integer, INetwork> networks = netFactory.getNetworks(NetworkFactory.NetworkType.DUMMY);
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
                OTMultResourcePool pool = new OTMultResourcePool(ots, finalI, comp_sec, statSec, networks.get(finalI), rand);
                // the constant mult be a 2-power larger than the amount of parties
                IMult mult = new IPSMult(pool, 32 * MODULO_BITLENGTH, safeExpansion);
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
        BigInteger[] A = new BigInteger[parties];
        BigInteger[] B = new BigInteger[parties];
        Map<Integer, IMult> mults = getMults(parties, COMP_SEC, STAT_SEC, false);
        Random rand = new Random(42);
        for (int i = 0; i < parties; i++) {
            A[i] = new BigInteger(MODULO_BITLENGTH, rand);
            B[i] = new BigInteger(MODULO_BITLENGTH, rand);
        }
        Field privateField = IPSMult.class.getDeclaredField("network");
        privateField.setAccessible(true);
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        List<Future<BigInteger>> C = new ArrayList<>(parties);
        for (int i = 0; i < parties; i++) {
            int finalI = i;
            C.add(executor.submit(() -> {
                ((DummyNetwork) privateField.get(mults.get(finalI))).resetCount();
                long start = System.currentTimeMillis();
                BigInteger res =mults.get(finalI).mult(A[finalI], B[finalI], MODULO);
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
        assertEquals(refA.multiply(refB).mod(MODULO), refC.mod(MODULO));


        DummyNetwork network = (DummyNetwork) privateField.get(mults.get(0));
        System.out.println("Rounds " + network.getRounds());
        System.out.println("Nettime " + network.getNetworkTime());
        System.out.println("Nettrans " + network.getTransfers());
        System.out.println("Net bytes " + network.getBytesSent());
    }
}
