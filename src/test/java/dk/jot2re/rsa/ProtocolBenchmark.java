package dk.jot2re.rsa;

import dk.jot2re.AbstractProtocolTest;
import dk.jot2re.mult.IMult;
import dk.jot2re.mult.MultFactory;
import dk.jot2re.mult.PlainMult;
import dk.jot2re.mult.ot.util.ExceptionConverter;
import dk.jot2re.network.INetwork;
import dk.jot2re.network.NetworkFactory;
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
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.io.Serializable;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static dk.jot2re.rsa.RSATestUtils.*;

@Fork( jvmArgs = {"-Xms512m", "-Xmx512m"})
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@BenchmarkMode({Mode.AverageTime})
@Warmup(iterations = 5, time = 5)
@Measurement(iterations = 10, time = 1)
@Timeout(time = 10)
public class ProtocolBenchmark extends AbstractProtocolTest {

    private static final int PARTIES = 3;
    private static final int STATSEC = 100;
    private static final int COMPSEC = 256;
    private static final String TYPE = "linear";
    private static final boolean PIVOT = true;
    private static final Random rand = ExceptionConverter.safe(()->SecureRandom.getInstance("SHA1PRNG", "SUN"), "Could not init random)");

    @State(Scope.Thread)
    public static class BenchState {
        public static BFProtocol bfProtocol;
        public static OurProtocol ourProtocol;
        public static IMult shamir;
        public static IMult replicated;
        public static BigInteger p;
        public static BigInteger q;
        public static BigInteger N;
        public static BigInteger A;
        public static BigInteger B;
        public static BigInteger Q;
        @Param({ "1024", "1536", "2048" })
        public static int BITS;

        @Setup(Level.Invocation)
        public void setupVariables() throws Exception {
            rand.setSeed(42);
            BigInteger p = BigInteger.probablePrime(BITS, rand);
            BigInteger q = BigInteger.probablePrime(BITS, rand);
            BigInteger N = p.multiply(q);

            BenchState.p = p;
            BenchState.q = q;
            BenchState.N = N;
            BenchState.A = new BigInteger(BITS, rand);
            BenchState.B = new BigInteger(BITS, rand);
            BenchState.Q = BigInteger.probablePrime(2*BITS+4, rand);

            BenchState.ourProtocol = setupOur(PARTIES, BITS, STATSEC, TYPE, PIVOT);
            BenchState.bfProtocol = setupBF(PARTIES, BITS, STATSEC, PIVOT);
            BenchState.shamir = setupShamir(PARTIES);
            BenchState.replicated = setupReplicated(PARTIES);
        }
    }

    public static void main(String[] args) throws Exception {
        Options opt = new OptionsBuilder()
                .include(ProtocolBenchmark.class.getSimpleName())
                .forks(1)
                .param(args[0], args[1])
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

    @Benchmark
    @Measurement(iterations = 10, time = 5)
    public boolean executeOur(BenchState state) throws Exception {
        return state.ourProtocol.execute(state.p, state.q, state.N);
    }


    public static IMult setupShamir(int parties) {
        MultFactory factory = new MultFactory(parties);
        Map<Integer, IMult> mults = factory.getMults(MultFactory.MultType.SHAMIR, NetworkFactory.NetworkType.PLAIN, false);
        NetworkFactory netFactory = new NetworkFactory(parties);
        Map<Integer, INetwork> networks = netFactory.getNetworks(NetworkFactory.NetworkType.PLAIN);
        mults.get(0).init(networks.get(0), rand);
        ((PlainNetwork) networks.get(0)).setDefaultResponse(BenchState.N.subtract(BigInteger.valueOf(123456789)));
        return mults.get(0);
    }

    public static IMult setupReplicated(int parties) {
        MultFactory factory = new MultFactory(parties);
        Map<Integer, IMult> mults = factory.getMults(MultFactory.MultType.REPLICATED, NetworkFactory.NetworkType.PLAIN, false);
        NetworkFactory netFactory = new NetworkFactory(parties);
        Map<Integer, INetwork> networks = netFactory.getNetworks(NetworkFactory.NetworkType.PLAIN);
        mults.get(0).init(networks.get(0), rand);
        ArrayList<BigInteger> resp = new ArrayList<>(PARTIES);
        for (int i = 0; i < PARTIES; i++) {
            resp.add(BenchState.N.subtract(BigInteger.valueOf(123456789*i+1)));
        }
        ((PlainNetwork) networks.get(0)).setDefaultResponse(resp);
        return mults.get(0);
    }


//    @Benchmark
//    @Measurement(iterations = 10, time = 1)
    public Serializable executeShamirMult(BenchState state) throws Exception {
        BigInteger res = BigInteger.ZERO;
        for (int i =0; i < 100; i++) {
            // operation to ensure actual computation don't get optimized away
            res =res.xor((BigInteger) state.shamir.multShares(state.A, state.B, state.Q));
        }
        return res;
    }

//    @Benchmark
//    @Measurement(iterations = 10, time = 1)
    public Serializable executeShamirInput(BenchState state) throws Exception {
        BigInteger res = BigInteger.ZERO;
        for (int i =0; i < 100; i++) {
            // operation to ensure actual computation don't get optimized away
            res =res.xor((BigInteger) state.shamir.shareFromAdditive(state.A, state.Q));
        }
        return res;
    }

    @Benchmark
    @Measurement(iterations = 10, time = 1)
    public Serializable executeReplicatedInput(BenchState state) throws Exception {
        ArrayList<BigInteger> res = new ArrayList<>(100);
        for (int i =0; i < 100; i++) {
            // operation to ensure actual computation don't get optimized away
            res.add(((ArrayList<BigInteger>) state.replicated.shareFromAdditive(state.A, state.Q)).get(0));
        }
        return res;
    }

    @Benchmark
    @Measurement(iterations = 10, time = 1)
    public void executeReplicatedMult(BenchState state, Blackhole hole) throws Exception {
        ArrayList<BigInteger> A = new ArrayList<>(PARTIES);
        for (int i = 0; i < PARTIES; i++) {
            A.add(BenchState.N.subtract(BigInteger.valueOf(123456789*i+1)));
        }
        for (int i =0; i < 100; i++) {
            // operation to ensure actual computation don't get optimized away
            hole.consume(state.replicated.multShares(A, A, state.Q));
        }
    }

    public static BFProtocol setupBF(int parties, int bitlength, int statsec, boolean pivot) {
        PlainNetwork network = new PlainNetwork(pivot ? 0 : 1, parties, pivot ? 0 : 1, null);
        BFParameters parameters = getBFBenchParameters(bitlength, statsec, parties, pivot ? 0 : 1);
        network.setDefaultResponse(BenchState.N.subtract(BigInteger.valueOf(123456789)));
        BFProtocol prot = new BFProtocol(parameters);
        prot.init(network, rand);
        return prot;
    }

    @Benchmark
    @Measurement(iterations = 10, time = 10)
    public boolean executeBF(BenchState state) throws Exception {
        return state.bfProtocol.execute(state.p, state.q, state.N);
    }

    public static OurParameters getOurBenchParameters(int bits, int statSec, int pivotId) {
        try {
            BigInteger M = MMap.get(bits);
            BigInteger P = PMap.get(bits);
            BigInteger Q = QMap.get(bits);
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
