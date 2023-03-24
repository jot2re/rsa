package dk.jot2re.rsa;

import dk.jot2re.AbstractProtocolTest;
import dk.jot2re.rsa.bf.BFParameters;
import dk.jot2re.rsa.bf.BFProtocol;
import dk.jot2re.rsa.our.OurParameters;
import dk.jot2re.rsa.our.OurProtocol;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.math.BigInteger;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

@BenchmarkMode({Mode.AverageTime})
@Warmup(iterations = 2, timeUnit =  TimeUnit.MILLISECONDS)
@Measurement(iterations = 1, timeUnit =  TimeUnit.MILLISECONDS)
//@Fork(jvmArgs = {"-Xms2G", "-Xmx2G"})
public class ProtocolBenchmark extends AbstractProtocolTest {
    public static void main(String[] args) throws Exception {
        Options opt = new OptionsBuilder()
                .include(ProtocolBenchmark.class.getSimpleName())
                .forks(1)
                .build();

        new Runner(opt).run();
    }

    @Benchmark
    public void runOurs2() throws Exception{
        benchmarkOur(3, 1024, 40, "log");
    }

    @Benchmark
    public void runTheirs2() throws Exception{
        benchmarkBf(3, 1024, 40);
    }

    private void benchmarkOur(int parties, int bitlength, int statsec,  String type) throws Exception {
        Map<Integer, BigInteger> pShares = RSATestUtils.randomPrime(parties, bitlength, new Random(42));
        Map<Integer, BigInteger> qShares = RSATestUtils.randomPrime(parties, bitlength, new Random(42));
        BigInteger p = pShares.values().stream().reduce(BigInteger.ZERO, (a, b) -> a.add(b));
        BigInteger q = qShares.values().stream().reduce(BigInteger.ZERO, (a, b) -> a.add(b));
        BigInteger N = p.multiply(q);

        AbstractProtocolTest.RunProtocol<Boolean> protocolRunner = (param) -> {
            OurProtocol.MembershipProtocol membership = switch (type) {
                case "linear" -> OurProtocol.MembershipProtocol.LINEAR;
                case "log" -> OurProtocol.MembershipProtocol.LOG;
                case "const" -> OurProtocol.MembershipProtocol.CONST;
                default -> throw new RuntimeException("unknown type");
            };
            OurProtocol protocol = new OurProtocol((OurParameters) param, membership);
            return protocol.execute(pShares.get(param.getMyId()), qShares.get(param.getMyId()), N);
        };

        AbstractProtocolTest.ResultCheck<Boolean> checker = (res) -> {
            for (Future<Boolean> cur : res) {
                assertTrue(cur.get());
            }
        };

        Map<Integer, OurParameters> parameters = RSATestUtils.getOurParameters(bitlength, statsec, parties);
        runProtocolTest(parameters, protocolRunner, checker);
    }

    private void benchmarkBf(int parties, int bitlength, int statsec) throws Exception {
        Map<Integer, BigInteger> pShares = RSATestUtils.randomPrime(parties, bitlength, rand);
        Map<Integer, BigInteger> qShares = RSATestUtils.randomPrime(parties, bitlength, rand);
        BigInteger p = pShares.values().stream().reduce(BigInteger.ZERO, (a, b)-> a.add(b));
        BigInteger q = qShares.values().stream().reduce(BigInteger.ZERO, (a, b)-> a.add(b));
        BigInteger N = p.multiply(q);

        RunProtocol<Boolean> protocolRunner = (param) -> {
            BFProtocol protocol = new BFProtocol((BFParameters) param);
            return protocol.execute(pShares.get(param.getMyId()), qShares.get(param.getMyId()), N);
        };

        ResultCheck<Boolean> checker = (res) -> {
            for (Future<Boolean> cur : res) {
                assertTrue(cur.get());
            }
        };

        Map<Integer, BFParameters> parameters = RSATestUtils.getBFParameters(bitlength, statsec, parties);
        runProtocolTest(parameters, protocolRunner, checker);
    }
}
