package dk.jot2re.mult.gilboa.ot;

import ch.qos.logback.classic.Level;
import dk.jot2re.mult.IMult;
import dk.jot2re.mult.PlainMult;
import dk.jot2re.mult.gilboa.GilboaMult;
import dk.jot2re.mult.gilboa.ot.otextension.*;
import dk.jot2re.mult.gilboa.util.AesCtrDrbg;
import dk.jot2re.mult.gilboa.util.AesCtrDrbgFactory;
import dk.jot2re.mult.gilboa.util.Pair;
import dk.jot2re.mult.gilboa.util.StrictBitVector;
import dk.jot2re.network.*;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class RotTest {
    private static final int COMP_SEC = 128;
    private static final int STAT_SEC = 40;
    private static final int AMOUNT = 1048576-COMP_SEC-STAT_SEC;

    public static <T extends INetwork> Map<Integer, T> getNetworks(int parties) {
        Map<Integer, PlainNetwork> map = new HashMap<>(parties);
        for (int i = 0; i < parties; i++) {
            PlainNetwork network = new PlainNetwork<>(i, 2, 0, null);
            network.init();
            map.put(i, network);
        }
        return (Map<Integer, T>) map;
    }

    public static Map<Integer, OtExtensionResourcePool> getOtParameters(int parties, int comp_sec, int statSec) {
        try {
            // todo generalize to more than 2
            DummyNetworkFactory netFactory = new DummyNetworkFactory(parties);
            Map<Integer, INetwork> networks = netFactory.getNetworks();

            ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
            Future<OtExtensionResourcePool> contextZero = executor.submit(() -> (new OtExtensionTestContext(0,1, comp_sec, statSec, networks.get(0))).createResources(0));
            Future<OtExtensionResourcePool> contextOne = executor.submit(() -> (new OtExtensionTestContext(1,0, comp_sec, statSec, networks.get(1))).createResources(0));
            executor.shutdown();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));

            Map<Integer, OtExtensionResourcePool> otResources = new HashMap<>(2);
            otResources.put(0, contextZero.get());
            otResources.put(1, contextOne.get());
            return otResources;
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Test
    void sunshine() throws Exception {
//        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
//        root.setLevel(Level.ALL);

        int parties = 2;
        Map<Integer, OtExtensionResourcePool> otResources = getOtParameters(parties, COMP_SEC, STAT_SEC);

        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        List<Future<?>> C = new ArrayList<>(parties);
        for (int i = 0; i < parties; i++) {
            int finalI = i;
            if (finalI == 0) {
                C.add(executor.submit(() -> {
                    RotFactory factory = new RotFactory(otResources.get(finalI), otResources.get(finalI).getNetwork());
                    RotSender sender = factory.createSender();
                    long start = System.currentTimeMillis();
                    Pair<List<StrictBitVector>, List<StrictBitVector>> res = sender.extend(AMOUNT);
                    long stop = System.currentTimeMillis();
                    System.out.println("sender " + (stop-start));
                    return res;
                }));
            } else {
                C.add(executor.submit(() -> {
                    RotFactory factory = new RotFactory(otResources.get(finalI), otResources.get(finalI).getNetwork());
                    RotReceiver receiver = factory.createReceiver();
                    StrictBitVector choices = new StrictBitVector(AMOUNT, AesCtrDrbgFactory.fromDerivedSeed((byte) 42));
                    long start = System.currentTimeMillis();
                    List<StrictBitVector> res = receiver.extend(choices);
                    long stop = System.currentTimeMillis();
                    System.out.println("receiver " + (stop-start));
                    return res;
                }));
            }
        }
        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));

        for (Future<?> cur : C) {
            assertNotNull(cur);
        }
    }
}
