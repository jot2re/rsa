package dk.jot2re.mult.shamir;

import dk.jot2re.mult.IMult;
import dk.jot2re.mult.ot.util.AesCtrDrbg;
import dk.jot2re.mult.ot.util.Drng;
import dk.jot2re.mult.ot.util.DrngImpl;
import dk.jot2re.network.DummyNetwork;
import dk.jot2re.network.DummyNetworkFactory;
import dk.jot2re.network.INetwork;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ShamirMultTest {
    // todo refactor and consolidate with Gilboa
    private static final int COMP_SEC = 128;
    private static final int STAT_SEC = 40;
    private static final int DEFAULT_BIT_LENGTH = 32;

    public static Map<Integer, IMult> getMults(int parties, int compSec, int statSec) throws ExecutionException, InterruptedException {
        DummyNetworkFactory netFactory = new DummyNetworkFactory(parties);
        Map<Integer, INetwork> networks = netFactory.getNetworks();
        Map<Integer, Future<IMult>> mults = new HashMap<>(parties);

        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        for (int i = 0; i <= parties; i++) {
            int finalI = i;
            Future<IMult> curMult = executor.submit(() -> {
                ShamirResourcePool pool = getResourcePool(finalI, parties, compSec, statSec);
                IMult mult = new ShamirMult(pool);
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

    public static ShamirResourcePool getResourcePool(int myId, int parties, int compSec, int statSec) {
        byte[] seed = new byte[32];
        seed[0] = (byte) myId;
        Drng rand = new DrngImpl(new AesCtrDrbg(seed));
        return new ShamirResourcePool(myId, parties, compSec, statSec, rand);
    }

    @ParameterizedTest
    @ValueSource(ints = {3,5})
    void sunshine(int parties) throws Exception {
        Random rand = new Random(42);
        BigInteger modulo = BigInteger.probablePrime(DEFAULT_BIT_LENGTH, rand);
        BigInteger[] A = new BigInteger[parties];
        BigInteger[] B = new BigInteger[parties];
        Map<Integer, IMult> mults = getMults(parties, COMP_SEC, STAT_SEC);
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
                System.out.println("Time: " + (stop-start));
                return res;
            }));
        }
        executor.shutdown();
        assertTrue(executor.awaitTermination(20000, TimeUnit.SECONDS));

        BigInteger refA = Arrays.stream(A).reduce(BigInteger.ZERO, BigInteger::add);
        BigInteger refB = Arrays.stream(B).reduce(BigInteger.ZERO, BigInteger::add);
        List<BigInteger> resShares = new ArrayList<>();
        for (Future<BigInteger> cur : C) {
            resShares.add(cur.get());
        }
        BigInteger refC = ((ShamirMult) mults.get(0)).combine(resShares.size()-1, resShares, modulo);
        assertEquals(refA.multiply(refB).mod(modulo), refC.mod(modulo));

        Field privateField = ShamirMult.class.getDeclaredField("network");
        privateField.setAccessible(true);
        DummyNetwork network = (DummyNetwork) privateField.get(mults.get(0));
        System.out.println("Rounds " + network.getRounds());
        System.out.println("Nettime " + network.getNetworkTime());
        System.out.println("Nettrans " + network.getTransfers());
        System.out.println("Net bytes " + network.getBytesSent());
    }
}
