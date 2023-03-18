package dk.jot2re;

import dk.jot2re.network.NetworkException;
import dk.jot2re.rsa.Parameters;
import dk.jot2re.rsa.RSATestUtils;
import dk.jot2re.rsa.bf.BFParameters;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class AbstractProtocolTest {
    protected static final int DEFAULT_BIT_LENGTH = 1024;
    protected static final int DEFAULT_STAT_SEC = 40;
    protected static final int DEFAULT_PARTIES = 3;
    protected static final Random rand = new Random(42);
    protected static final BigInteger modulo = BigInteger.probablePrime(DEFAULT_BIT_LENGTH, rand);


    public <T> void runProtocolTest(int primeBits, int statSec, int parties, RunProtocol<T> protocolRunner, ResultCheck<T> resultChecker) throws Exception {
        // NOTE: ENABLE FOR DEBUGGING
        //        DummyNetwork.TIME_OUT_MS = 100000000;
        Map<Integer, BFParameters> params = RSATestUtils.getParameters(primeBits, statSec, parties);
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        List<Future<T>> res = new ArrayList<>(parties);
        for (int i = 0; i < parties; i++) {
            int finalI = i;
            res.add(executor.submit(() -> {
                try {
                    return protocolRunner.apply(params.get(finalI));
                } catch (Exception e) {
                    System.err.println("Error: " + e.getMessage());
                    throw new RuntimeException(e);
                }
            }));
        }
        executor.shutdown();
        assertTrue(executor.awaitTermination(10000, TimeUnit.SECONDS));

        resultChecker.check(res);
    }

    @FunctionalInterface
    public interface RunProtocol<OutputT> {
        OutputT apply(Parameters params) throws NetworkException;
    }

    @FunctionalInterface
    public interface ResultCheck<ResultT> {
        void check(List<Future<ResultT>> params) throws ExecutionException, InterruptedException;
    }
}
