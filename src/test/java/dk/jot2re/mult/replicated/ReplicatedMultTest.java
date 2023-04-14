package dk.jot2re.mult.replicated;

import dk.jot2re.mult.IMult;
import dk.jot2re.mult.ot.util.AesCtrDrbg;
import dk.jot2re.mult.ot.util.Drng;
import dk.jot2re.mult.ot.util.DrngImpl;
import dk.jot2re.network.DummyNetwork;
import dk.jot2re.network.NetworkFactory;
import dk.jot2re.network.INetwork;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.*;

import static dk.jot2re.DefaultSecParameters.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ReplicatedMultTest {
    // todo refactor and consolidate with Gilboa

    public static Map<Integer, IMult> getMults(int parties, int compSec, int statSec) throws ExecutionException, InterruptedException {
        NetworkFactory netFactory = new NetworkFactory(parties);
        Map<Integer, INetwork> networks = netFactory.getNetworks(NetworkFactory.NetworkType.DUMMY);
        Map<Integer, Future<IMult>> mults = new HashMap<>(parties);

        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        for (int i = 0; i <= parties; i++) {
            int finalI = i;
            Future<IMult> curMult = executor.submit(() -> {
                ReplictedMultResourcePool pool = getResourcePool(finalI, parties, compSec, statSec);
                IMult mult = new ReplicatedMult(pool);
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

    public static ReplictedMultResourcePool getResourcePool(int myId, int parties, int compSec, int statSec) {
        byte[] seed = new byte[32];
        seed[0] = (byte) myId;
        Drng rand = new DrngImpl(new AesCtrDrbg(seed));
        return new ReplictedMultResourcePool(myId, parties, compSec, statSec, rand);
    }

    @ParameterizedTest
    @ValueSource(ints = {3})
    void sunshine(int parties) throws Exception {
        BigInteger[] A = new BigInteger[parties];
        BigInteger[] B = new BigInteger[parties];
        Map<Integer, IMult> mults = getMults(parties, COMP_SEC, STAT_SEC);
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
                long start = System.currentTimeMillis();
                BigInteger res =mults.get(finalI).mult(A[finalI], B[finalI], MODULO);
                long stop = System.currentTimeMillis();
                System.out.println("Time: " + (stop-start));
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

        Field privateField = ReplicatedMult.class.getDeclaredField("network");
        privateField.setAccessible(true);
        DummyNetwork network = (DummyNetwork) privateField.get(mults.get(0));
        System.out.println("Rounds " + network.getRounds());
        System.out.println("Nettime " + network.getNetworkTime());
        System.out.println("Nettrans " + network.getTransfers());
        System.out.println("Net bytes " + network.getBytesSent());
    }

    @Test
    void testShare() {
        ReplictedMultResourcePool pool = getResourcePool(0, 3, COMP_SEC, STAT_SEC);
        ReplicatedMult mult = new ReplicatedMult(pool);
        List<BigInteger> shares = mult.share(BigInteger.valueOf(42), BigInteger.valueOf(123456789));
        assertEquals(BigInteger.valueOf(42),
                shares.stream().reduce(BigInteger.ZERO, (a,b)->a.add(b).mod(BigInteger.valueOf(123456789))));
    }

    @Test
    void testPartyShares() {
        ReplictedMultResourcePool pool = getResourcePool(0, 3, COMP_SEC, STAT_SEC);
        ReplicatedMult mult = new ReplicatedMult(pool);
        List<BigInteger> shares = Arrays.asList(BigInteger.valueOf(1), BigInteger.valueOf(2), BigInteger.valueOf(3));
        List<BigInteger> partyShares = mult.getPartyShares(0, shares);
        assertEquals(partyShares.get(0), BigInteger.valueOf(1));
        assertEquals(partyShares.get(1), BigInteger.valueOf(2));

        partyShares = mult.getPartyShares(1, shares);
        assertEquals(partyShares.get(0), BigInteger.valueOf(2));
        assertEquals(partyShares.get(1), BigInteger.valueOf(3));

        partyShares = mult.getPartyShares(2, shares);
        assertEquals(partyShares.get(0), BigInteger.valueOf(3));
        assertEquals(partyShares.get(1), BigInteger.valueOf(1));
    }

    @Test
    void testShareInput() throws Exception {
        int parties = 3;
        BigInteger[] A = new BigInteger[parties];
        Map<Integer, IMult> mults = getMults(parties, COMP_SEC, STAT_SEC);
        for (int i = 0; i < parties; i++) {
            A[i] = new BigInteger(MODULO_BITLENGTH, new Random(42));
        }
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        List<Future<List<BigInteger>>> sharedInput = new ArrayList<>(parties);
        for (int i = 0; i < parties; i++) {
            int finalI = i;
            sharedInput.add(executor.submit(() -> ((ReplicatedMult) mults.get(finalI)).sharedInput(A[finalI], MODULO)));
        }
        executor.shutdown();
        assertTrue(executor.awaitTermination(20000, TimeUnit.SECONDS));

        // Get the shared value from party 0 and 1. I.e. the shares of party 0 and last share of party 1
        BigInteger computedSum = sharedInput.get(0).get().get(0).add(sharedInput.get(0).get().get(1)).add(sharedInput.get(1).get().get(1));
        assertEquals(
                Arrays.stream(A).reduce(BigInteger.ZERO, (a,b)->a.add(b).mod(MODULO)),
                computedSum.mod(MODULO));
    }
}
