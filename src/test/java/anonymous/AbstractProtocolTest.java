package anonymous;

import anonymous.network.INetwork;
import anonymous.network.NetworkException;
import anonymous.rsa.Parameters;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class AbstractProtocolTest {
    protected static final int DEFAULT_PARTIES = 3;
    protected static final Random rand = new Random(42);

    public <ReturnT, ParameterT extends Parameters> void runProtocolTest(Map<Integer, INetwork> networks, Map<Integer, ParameterT> params, RunProtocol<ReturnT> protocolRunner, ResultCheck<ReturnT> resultChecker) throws Exception {
        // NOTE: ENABLE FOR DEBUGGING
        //        DummyNetwork.TIME_OUT_MS = 100000000;
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(networks.size());
        List<Future<ReturnT>> res = new ArrayList<>(params.size());
        for (int i = 0; i < params.size(); i++) {
            int finalI = i;
            res.add(executor.submit(() -> {
                try {
                    return protocolRunner.apply(params.get(finalI), networks.get(finalI));
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
        OutputT apply(Parameters params, INetwork network) throws NetworkException;
    }

    @FunctionalInterface
    public interface ResultCheck<ResultT> {
        void check(List<Future<ResultT>> params) throws ExecutionException, InterruptedException;
    }
}
