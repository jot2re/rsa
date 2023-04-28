package dk.jot2re.mult.replicated;

import dk.jot2re.mult.IMult;
import dk.jot2re.mult.MultCounter;
import dk.jot2re.mult.MultFactory;
import dk.jot2re.network.DummyNetwork;
import dk.jot2re.network.INetwork;
import dk.jot2re.network.NetworkFactory;
import dk.jot2re.rsa.RSATestUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static dk.jot2re.DefaultSecParameters.COMP_SEC;
import static dk.jot2re.DefaultSecParameters.STAT_SEC;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ReplicatedMultTest {
    // todo refactor and consolidate with Gilboa

    public static ReplictedMultResourcePool getResourcePool(int myId, int parties, int compSec, int statSec) {
        return new ReplictedMultResourcePool(myId, parties, compSec, statSec);
    }

    @ParameterizedTest
    @ValueSource(ints = {3})
    void sunshine(int parties) throws Exception {
        int bitlength = 1536+4;
        Random rand = new Random(42);
        BigInteger modulo =  BigInteger.probablePrime(bitlength, rand);
        ArrayList<BigInteger> A = new ArrayList<>(parties);
        ArrayList<BigInteger> B = new ArrayList<>(parties);
        MultFactory factory = new MultFactory(parties);
        Map<Integer, IMult> mults = factory.getMults(MultFactory.MultType.REPLICATED, NetworkFactory.NetworkType.DUMMY, true);
        Map<Integer, INetwork> nets = RSATestUtils.getNetworks(parties);
        for (int i = 0; i < parties; i++) {
            A.add(new BigInteger(bitlength, rand));
            B.add(new BigInteger(bitlength, rand));
        }
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        List<Future<Serializable>> C = new ArrayList<>(parties);
        for (int i = 0; i < parties; i++) {
            int finalI = i;
            C.add(executor.submit(() -> {
                mults.get(finalI).init(nets.get(finalI), RSATestUtils.getRandom(finalI));
                long start = System.currentTimeMillis();
                Serializable res =mults.get(finalI).mult(A.get(finalI), B.get(finalI), modulo);
                long stop = System.currentTimeMillis();
                System.out.println("Time: " + (stop-start));
                return res;
            }));
        }
        executor.shutdown();
        assertTrue(executor.awaitTermination(20000, TimeUnit.SECONDS));
        System.out.println(((MultCounter) mults.get(0)).toString());

        BigInteger refA = A.stream().reduce(BigInteger.ZERO, BigInteger::add);
        BigInteger refB = B.stream().reduce(BigInteger.ZERO, BigInteger::add);
        BigInteger refC = BigInteger.ZERO;
        for (Future<Serializable> cur : C) {
            refC = refC.add((BigInteger) cur.get());
        }
        assertEquals(refA.multiply(refB).mod(modulo), refC.mod(modulo));

        Field privateField = MultCounter.class.getDeclaredField("network");
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
        mult.init(null, new Random(42));
        List<BigInteger> shares = mult.singlePartyShare(BigInteger.valueOf(42), BigInteger.valueOf(123456789));
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
}
