package dk.jot2re.rsa;

import dk.jot2re.AbstractProtocolTest;
import dk.jot2re.mult.IMult;
import dk.jot2re.mult.PlainMult;
import dk.jot2re.mult.ot.util.ExceptionConverter;
import dk.jot2re.network.PlainNetwork;
import dk.jot2re.rsa.bf.BFParameters;
import dk.jot2re.rsa.bf.BFProtocol;
import dk.jot2re.rsa.our.OurParameters;
import dk.jot2re.rsa.our.OurProtocol;
import dk.jot2re.rsa.our.sub.invert.Invert;
import dk.jot2re.rsa.our.sub.membership.IMembership;
import dk.jot2re.rsa.our.sub.membership.MembershipConst;
import dk.jot2re.rsa.our.sub.membership.MembershipLinear;
import dk.jot2re.rsa.our.sub.membership.MembershipLog;
import dk.jot2re.rsa.our.sub.multToAdd.MultToAdd;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.TimeUnit;

//@Fork(jvmArgs = {"-Xms2G", "-Xmx2G"})
public class ProtocolBenchmark extends AbstractProtocolTest {

    private static final int BITS = 1024;
    private static final int PARTIES = 3;
    private static final int STATSEC = 40;
    private static final String TYPE = "linear";
    private static final boolean PIVOT = true;
    private static final Random rand = ExceptionConverter.safe(()->SecureRandom.getInstance("SHA1PRNG", "SUN"), "Could not init random)");

    @State(Scope.Thread)
    public static class BenchState {
        public static BFProtocol bfProtocol;
        public static OurProtocol ourProtocol;
        public static BigInteger p;
        public static BigInteger q;
        public static BigInteger N;

        @Setup(Level.Invocation)
        public void setupVariables() throws Exception {
            rand.setSeed(42);
            BigInteger p = BigInteger.probablePrime(BITS, rand);
            BigInteger q = BigInteger.probablePrime(BITS, rand);
            BigInteger N = p.multiply(q);

            BenchState.p = p;
            BenchState.q = q;
            BenchState.N = N;

            BenchState.ourProtocol = setupOur(PARTIES, BITS, STATSEC, TYPE, PIVOT);
            BenchState.bfProtocol = setupBF(PARTIES, BITS, STATSEC, PIVOT);
        }
    }

    public static void main(String[] args) throws Exception {
        Options opt = new OptionsBuilder()
                .include(ProtocolBenchmark.class.getSimpleName())
                .forks(1)
                .build();

        new Runner(opt).run();
    }

    public static OurProtocol setupOur(int parties, int bitlength, int statsec, String type, boolean pivot) throws Exception {
        OurParameters parameters = getOurBenchParameters(bitlength, statsec, pivot ? 0 : 1);
        IMembership membership = switch (type) {
            case "const" -> new MembershipConst(parameters);
            case "log" -> new MembershipLog(parameters);
            case "linear" ->  new MembershipLinear(parameters);
            default -> throw new IllegalArgumentException("Unknown membership protocol");
        };
        MultToAdd mta = new MultToAdd(parameters);
        PlainNetwork mtaNetwork = new PlainNetwork(pivot ? 0 : 1, parties, pivot ? 0 : 1, null);
        // Change the default value to ensure that things work multiplicatively, although still not correctly
        mtaNetwork.setDefaultResponse(BenchState.N.subtract(BigInteger.valueOf(123456789)));
        OurProtocol prot = new OurProtocol(parameters, membership, new Invert(parameters), mta);
        PlainNetwork network = new PlainNetwork(pivot ? 0 : 1, parties, pivot ? 0 : 1, null);
        // Set values to something large to ensure that things takes as long as in real executions
        network.setDefaultResponse(BenchState.N.subtract(BigInteger.valueOf(123456789)));
        prot.init(network, rand);
        return prot;
    }

    @Fork(value = 1, warmups = 2)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    @Benchmark
    @BenchmarkMode({Mode.AverageTime})
    public boolean executeOur(BenchState state) throws Exception {
        return state.ourProtocol.execute(state.p, state.q, state.N);
    }

    public static BFProtocol setupBF(int parties, int bitlength, int statsec, boolean pivot) {
        PlainNetwork network = new PlainNetwork(pivot ? 0 : 1, parties, pivot ? 0 : 1, null);
        BFParameters parameters = getBFBenchParameters(bitlength, statsec, parties, pivot ? 0 : 1);
        network.setDefaultResponse(BenchState.N.subtract(BigInteger.valueOf(123456789)));
        BFProtocol prot = new BFProtocol(parameters);
        prot.init(network, rand);
        return prot;
    }

//    @Fork(value = 1, warmups = 2)
//@OutputTimeUnit(TimeUnit.MICROSECONDS)
//    @Benchmark
//    @BenchmarkMode({Mode.AverageTime})
    public boolean executeBF(BenchState state) throws Exception {
        return state.bfProtocol.execute(state.p, state.q, state.N);
    }

    public static OurParameters getOurBenchParameters(int bits, int statSec, int pivotId) {
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
            return new OurParameters(bits, statSec, P, Q, M, pivotMult);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public static BFParameters getBFBenchParameters(int bits, int statSec, int parties, int pivotId) {
        try {
            // Unique but deterministic seed for each set of parameters
            SecureRandom rand = SecureRandom.getInstance("SHA1PRNG", "SUN");
            // Note that seed is only updated if different from 0
            rand.setSeed(pivotId);
            IMult pivotMult = new PlainMult(pivotId);
            List<Map<Integer, ArrayList<BigInteger>>> emulatedMsgs = new ArrayList<>(1);
            Map<Integer, ArrayList<BigInteger>> map = new HashMap<>();
            for (int j = 0; j < parties; j++) {
                ArrayList<BigInteger> array = new ArrayList<>();
                for (int i = 0; i < statSec; i++) {
                    // Large random numbers
                    array.add(new BigInteger(bits, rand));
                }
                map.put(j, array);
            }
            emulatedMsgs.add(map);
            return new BFParameters(bits, statSec, pivotMult);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
