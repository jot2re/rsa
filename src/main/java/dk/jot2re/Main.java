package dk.jot2re;

import dk.jot2re.mult.IMult;
import dk.jot2re.mult.MultFactory;
import dk.jot2re.network.DummyNetwork;
import dk.jot2re.network.NetworkFactory;

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

import static dk.jot2re.DefaultSecParameters.findMaxPrime;

public class Main {
    public static void main(String[] args) throws Exception {
        Main test = new Main();
        test.sunshine(2, 1032);
        test.sunshine(2, 1544);
        test.sunshine(2, 2056);
    }

    void sunshine(int parties, int bitlength) throws Exception {
        Random rand = new Random(42);
        BigInteger modulo = findMaxPrime(bitlength);
        BigInteger[] A = new BigInteger[parties];
        BigInteger[] B = new BigInteger[parties];
        MultFactory factory = new MultFactory(parties);
        Map<Integer, IMult> mults = factory.getMults(MultFactory.MultType.IPS, NetworkFactory.NetworkType.DUMMY, false);
        for (int i = 0; i < parties; i++) {
            A[i] = new BigInteger(bitlength-2, rand);
            B[i] = new BigInteger(bitlength-2, rand);
        }
        System.out.println("parites " + parties + ", bitlength " + bitlength);
        Field privateField = AbstractProtocol.class.getDeclaredField("network");
        privateField.setAccessible(true);
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        List<Future<BigInteger>> C = new ArrayList<>(parties);
        for (int i = 0; i < parties; i++) {
            int finalI = i;
            C.add(executor.submit(() -> {
                BigInteger res = null;
                // Warmup
                for (int j = 0;j < 100; j++) {
                    res = mults.get(finalI).mult(A[finalI], B[finalI], modulo);
                }
                ((DummyNetwork) privateField.get(mults.get(finalI))).resetCount();
                // TODO standard deviation
                List<Long> times = new ArrayList<>();
                long sum = 0l;
                int amount = 20;
                for (int j = 0;j < amount; j++) {
                    long start = System.nanoTime();
                    res = mults.get(finalI).mult(A[finalI], B[finalI], modulo);
                    long stop = System.nanoTime();
                    sum += (stop-start)/1000;
                    times.add((stop-start)/1000);
                }
                long avg = sum/amount;
                long stdSum = 0l;
                for (long cur : times) {
                    stdSum += (cur-avg)*(cur-avg);
                }
                double std = Math.sqrt(stdSum/amount);
                System.out.println("sender " + avg + " std " + std);
//                System.out.println(Arrays.toString(times.toArray()));
//                System.out.println("min " + Collections.min(times));
//                System.out.println("max " + Collections.max(times));
                return res;
            }));
        }
        executor.shutdown();
//        System.out.println(((MultCounter) mults.get(0)).toString());

        BigInteger refA = Arrays.stream(A).reduce(BigInteger.ZERO, BigInteger::add);
        BigInteger refB = Arrays.stream(B).reduce(BigInteger.ZERO, BigInteger::add);
        BigInteger refC = BigInteger.ZERO;
        for (Future<BigInteger> cur : C) {
            refC = refC.add(cur.get());
        }

        DummyNetwork network = (DummyNetwork) privateField.get(mults.get(0));
        System.out.println("Nettime " + network.getNetworkTime());
    }
}