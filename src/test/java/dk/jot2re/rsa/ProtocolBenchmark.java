package dk.jot2re.rsa;

import dk.jot2re.AbstractProtocolTest;
import dk.jot2re.mult.IMult;
import dk.jot2re.mult.PlainMult;
import dk.jot2re.network.INetwork;
import dk.jot2re.network.PlainNetwork;
import dk.jot2re.rsa.bf.BFParameters;
import dk.jot2re.rsa.bf.BFProtocol;
import dk.jot2re.rsa.our.OurParameters;
import dk.jot2re.rsa.our.OurProtocol;
import dk.jot2re.rsa.our.sub.membership.IMembership;
import dk.jot2re.rsa.our.sub.membership.MembershipConst;
import dk.jot2re.rsa.our.sub.membership.MembershipLinear;
import dk.jot2re.rsa.our.sub.membership.MembershipLog;
import org.junit.jupiter.api.Test;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.math.BigInteger;
import java.security.SecureRandom;
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

//    @Benchmark
    @Test
    public void runOurs2() throws Exception{
        benchmarkOur(2, 1024, 40, "linear", true);
    }

//    @Benchmark
    @Test
    public void runTheirs2() throws Exception{
        benchmarkBf(3, 1024, 40);
    }

    private void benchmarkOur(int parties, int bitlength, int statsec,  String type, boolean pivot) throws Exception {
        Map<Integer, BigInteger> pShares = RSATestUtils.randomPrime(parties, bitlength, new Random(42));
        Map<Integer, BigInteger> qShares = RSATestUtils.randomPrime(parties, bitlength, new Random(42));
        BigInteger p = pShares.values().stream().reduce(BigInteger.ZERO, (a, b) -> a.add(b));
        BigInteger q = qShares.values().stream().reduce(BigInteger.ZERO, (a, b) -> a.add(b));
        BigInteger N = p.multiply(q);

        OurParameters parameters = getOurBenchParameters(bitlength, statsec, parties, pivot ? 0 : 1);
        IMembership membership = switch (type) {
            case "const" -> new MembershipConst(parameters);
            case "log" -> new MembershipLog(parameters);
            case "linear" ->  new MembershipLinear(parameters);
            default -> throw new IllegalArgumentException("Unknown membership protocol");
        };
        OurParameters multToAddParams = getOurBenchParameters(bitlength, statsec, parties, pivot ? 0 : 1);
        // Change the default value to ensure that things work multiplicatively
        ((PlainNetwork) parameters.getNetwork()).setDefaultResponse(BigInteger.ONE);
        long before = System.currentTimeMillis();
        OurProtocol protocol = new OurProtocol(parameters,  OurProtocol.MembershipProtocol.LINEAR);//new OurProtocol(parameters, membership, new Invert(parameters), new MultToAdd(multToAddParams));
        boolean res = protocol.execute(p, q, N);
        long after = System.currentTimeMillis();
        System.out.println(after-before);
        assertTrue(res);
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

    public static OurParameters getOurBenchParameters(int bits, int statSec, int parties, int pivotId) {
        try {
            // M > 2^2*bits TODO is that correct?
            BigInteger M = RSATestUtils.prime(2*bits+1, new Random(42));
            // P > mN, we assume at most 2048 parties
            BigInteger P = RSATestUtils.prime(2*bits+11, new Random(42));
            // Q > P
            BigInteger Q = RSATestUtils.prime(2*bits+12, new Random(42));
            // Unique but deterministic seed for each set of parameters
            SecureRandom rand = SecureRandom.getInstance("SHA1PRNG", "SUN");
            // Note that seed is only updated if different from 0
            rand.setSeed(pivotId);
            IMult pivotMult = new PlainMult(pivotId);
            INetwork network = new PlainNetwork(pivotId, parties, pivotId);
            network.init();
            pivotMult.init(network);
            return new OurParameters(bits, statSec, P, Q, M, network, pivotMult, rand);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
