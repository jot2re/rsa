package anonymous;

import anonymous.compiler.*;
import anonymous.mult.IMult;
import anonymous.mult.MultFactory;
import anonymous.mult.ot.util.ExceptionConverter;
import anonymous.network.DummyNetwork;
import anonymous.network.NetworkFactory;
import anonymous.rsa.our.OurParameters;
import anonymous.rsa.our.OurProtocol;
import anonymous.rsa.our.RSAUtil;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.*;

import static anonymous.DefaultSecParameters.COMP_SEC;
import static anonymous.DefaultSecParameters.STAT_SEC;

public class Main {
    public static final NetworkFactory.NetworkType NETWORK_TYPE = NetworkFactory.NetworkType.DUMMY;
    public static void main(String[] args) throws Exception {
        postprocessing(2048);
        postprocessing(3072);
        postprocessing(4096);
    }

    public static void benchCompiler(int bitlength) throws Exception {
        System.out.println(bitlength);
        Random rand = new Random(42);
        int parties = 3;
        Map<Integer, BigInteger> pShares = randomPrime(parties, bitlength/2, rand);
        Map<Integer, BigInteger> qShares = randomPrime(parties, bitlength/2, rand);
        BigInteger p = pShares.values().stream().reduce(BigInteger.ZERO, (a, b) -> a.add(b));
        BigInteger q = qShares.values().stream().reduce(BigInteger.ZERO, (a, b) -> a.add(b));
        BigInteger N = p.multiply(q);

        // NOTE that we need copies of both, since the network for mult is stored in the parameters!!! Terrible decision! TODO FIX!
        Map<Integer, OurParameters> brainParameters = getOurParameters(bitlength/2, STAT_SEC, parties, false, MultFactory.MultType.REPLICATED);
        Map<Integer, OurParameters> pinkyParameters = getOurParameters(bitlength/2, STAT_SEC, parties, false, MultFactory.MultType.REPLICATED);
        CompiledNetworkFactory netFactory = new CompiledNetworkFactory(new NetworkFactory(parties));
        Map<Integer, NetworkPair> networks = netFactory.getNetworks(NetworkFactory.NetworkType.DUMMY);
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        List<Future<BigInteger>> res = new ArrayList<>(parties);
        for (int i = 0; i < parties; i++) {
            int finalI = i;
            res.add(executor.submit(() -> {
                OurProtocol.MembershipProtocol membership = OurProtocol.MembershipProtocol.LOG;
                CompiledProtocolResources resources = new CompiledProtocolResources(COMP_SEC);
                BigInteger temp = BigInteger.ZERO;
                for (int j = 0; j < 15; j++) {
                    CompiledProtocol protocol = new CompiledProtocol(resources, new OurProtocol(brainParameters.get(finalI), membership), new OurProtocol(pinkyParameters.get(finalI), membership));
                    protocol.init(networks.get(finalI), getRandom(finalI));
                    BigInteger localRes = protocol.execute(Arrays.asList(pShares.get(finalI), qShares.get(finalI)), Arrays.asList(N)).get(0);
                    // ensure things get run
                    temp = temp.add(localRes);
                }
                long time = 0;
                Field privateField = AbstractProtocol.class.getDeclaredField("network");
                privateField.setAccessible(true);
                ((DummyNetwork) ((PinkyNetwork) privateField.get(pinkyParameters.get(finalI).getMult())).internalNetwork).resetCount();
                long max = 0;
                for (int j = 0; j < 30; j++) {
                    CompiledProtocol protocol = new CompiledProtocol(resources, new OurProtocol(brainParameters.get(finalI), membership), new OurProtocol(pinkyParameters.get(finalI), membership));
                    long start = System.currentTimeMillis();
                    protocol.init(networks.get(finalI), getRandom(finalI));
                    BigInteger localRes = protocol.execute(Arrays.asList(pShares.get(finalI), qShares.get(finalI)), Arrays.asList(N)).get(0);
                    long stop = System.currentTimeMillis();
                    // ensure things get run
                    temp = temp.add(localRes);
                    time+= stop-start;
                    if (stop-start > max) {
                        max = stop-start;
                    }
                }
                System.out.println("max " + max);
                System.out.println("time: " + ((double) time/30));
                return BigInteger.valueOf(time) ;
            }));
        }
        executor.shutdown();
        executor.awaitTermination(20000, TimeUnit.SECONDS);

//        //TODO ensure correctness of pinky com! Result should 1!
//        for (Future<BigInteger> cur : res) {
//            assertEquals(BigInteger.ONE, cur.get());
//        }

//        runProtocolTest(brainNets, pinkyNets, parameters, protocolRunner, checker);
//        System.out.println(((MultCounter) parameters.get(0).getMult()).toString());
        System.out.println("Nettime " + (((double) ((DummyNetwork) networks.get(0).getBrainNetwork().internalNetwork).getNetworkTime())
                +((DummyNetwork) networks.get(0).getPinkyNetwork().internalNetwork).getNetworkTime())/30);
        System.out.println("Net bytes " + (((double) ((DummyNetwork) networks.get(0).getBrainNetwork().internalNetwork).getBytesSent()
                + ((DummyNetwork) networks.get(0).getPinkyNetwork().internalNetwork).getBytesSent())/30));
    }

    public static void postprocessing(int bitlength) throws Exception {
        System.out.println(bitlength);
        int parties = 3;
        Random rand = new Random(41);
        BigInteger modulo = BigInteger.probablePrime(bitlength+4, rand);
        BigInteger a = RSAUtil.sample(rand, modulo);
        BigInteger b = RSAUtil.sample(rand, modulo);
        BigInteger c = a.multiply(b).mod(modulo);
        Map<Integer, BigInteger> aShares = share(a, parties, modulo, rand);
        Map<Integer, BigInteger> bShares = share(b, parties, modulo, rand);
        Map<Integer, BigInteger> cShares = share(c, parties, modulo, rand);

        Map<Integer, OurParameters> sharingParams = getOurParameters(bitlength, STAT_SEC, parties, false, MultFactory.MultType.REPLICATED);
        Map<Integer, OurParameters> brainParameters = getOurParameters(bitlength, STAT_SEC, parties, false, MultFactory.MultType.REPLICATED);
        Map<Integer, OurParameters> pinkyParameters = getOurParameters(bitlength, STAT_SEC, parties, false, MultFactory.MultType.REPLICATED);
        CompiledNetworkFactory netFactory = new CompiledNetworkFactory(new NetworkFactory(parties));
        Map<Integer, NetworkPair> networks = netFactory.getNetworks(NetworkFactory.NetworkType.DUMMY);
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        List<Future<Boolean>> res = new ArrayList<>(parties);
        for (int i = 0; i < parties; i++) {
            int finalI = i;
            res.add(executor.submit(() -> {
                PostprocessingProtocol protocol = new PostprocessingProtocol(brainParameters.get(finalI), pinkyParameters.get(finalI), COMP_SEC);
                protocol.init(networks.get(finalI), getRandom(networks.get(finalI).getBrainNetwork().myId()));
                sharingParams.get(finalI).getMult().init(networks.get(finalI).getBrainNetwork().internalNetwork, getRandom(networks.get(finalI).getBrainNetwork().myId()));
                Serializable myAShare = sharingParams.get(finalI).getMult().shareFromAdditive(aShares.get(networks.get(finalI).getBrainNetwork().myId()), modulo);
                Serializable myBShare = sharingParams.get(finalI).getMult().shareFromAdditive(bShares.get(networks.get(finalI).getBrainNetwork().myId()), modulo);
                Serializable myCShare = sharingParams.get(finalI).getMult().shareFromAdditive(cShares.get(networks.get(finalI).getBrainNetwork().myId()), modulo);
                boolean curRes = true;
                for (int j = 0; j < 15; j++) {
                    curRes ^= protocol.execute(Arrays.asList(new PostprocessingProtocol.Multiplication(myAShare, myBShare, myCShare, modulo)));
                }
                long time = 0;
                Field privateField = AbstractProtocol.class.getDeclaredField("network");
                privateField.setAccessible(true);
                ((DummyNetwork) ((PinkyNetwork) privateField.get(pinkyParameters.get(finalI).getMult())).internalNetwork).resetCount();
                long max = 0;
                for (int j = 0; j < 30; j++) {
                    long start = System.currentTimeMillis();
                    for (int k = 0; k < 27; k++) {
                        curRes ^= protocol.execute(Arrays.asList(new PostprocessingProtocol.Multiplication(myAShare, myBShare, myCShare, modulo)));
                    }
                    long stop = System.currentTimeMillis();
                    time+= stop-start;
                    if (stop-start > max) {
                        max = stop-start;
                    }
                }
                System.out.println("max " + max);
                System.out.println("time: " + ((double) time/30));
                return curRes;
            }));
        }
        executor.shutdown();
        executor.awaitTermination(20000, TimeUnit.SECONDS);

        System.out.println("Nettime " + (((double) ((DummyNetwork) networks.get(0).getBrainNetwork().internalNetwork).getNetworkTime())
                +((DummyNetwork) networks.get(0).getPinkyNetwork().internalNetwork).getNetworkTime())/30);
        System.out.println("Net bytes " + (((double) ((DummyNetwork) networks.get(0).getBrainNetwork().internalNetwork).getBytesSent()
                + ((DummyNetwork) networks.get(0).getPinkyNetwork().internalNetwork).getBytesSent())/30));
    }

    public static Random getRandom(int myId) {
        SecureRandom random = ExceptionConverter.safe( ()-> SecureRandom.getInstance("SHA1PRNG", "SUN"), "Could not get random");
        random.setSeed(myId);
        return random;
    }

    public static Map<Integer, BigInteger> randomPrime(int parties, int bitLength, Random rand) {
        BigInteger prime = prime(bitLength, rand);
        Map<Integer, BigInteger> shares = new HashMap<>(parties);
        // We sample a number small enough to avoid issues with negative shares
        for (int party = 1; party < parties; party++) {
            shares.put(party, (new BigInteger(bitLength - 4 - parties, rand)).multiply(BigInteger.valueOf(4)));
        }
        shares.put(0, prime.subtract(shares.values().stream().reduce(BigInteger.ZERO, (a, b)-> a.add(b))));
        return shares;
    }

    public static Map<Integer, OurParameters> getOurParameters(int bits, int statSec, int parties, boolean decorated, MultFactory.MultType multType) {
        try {
            // TODO the 8 increments are needed for OT mult protocols but not others
            // M > 2^(2*bits)
            BigInteger M = BigInteger.probablePrime(2*bits+8, new Random(42));
            // P > mN, we assume at most 2048 parties
            BigInteger P = BigInteger.probablePrime(2*bits+16, new Random(42));
            // Q > P
            BigInteger Q = BigInteger.probablePrime(2*bits+24, new Random(42));
            MultFactory multFactory = new MultFactory(parties);
            Map<Integer, OurParameters> params = new HashMap<>(parties);
            Map<Integer, IMult> mults = multFactory.getMults(multType, NETWORK_TYPE, decorated);
            for (int i = 0; i < parties; i++) {
                // Unique but deterministic seed for each set of parameters
                SecureRandom rand = SecureRandom.getInstance("SHA1PRNG", "SUN");
                // Note that seed is only updated if different from 0
                rand.setSeed(i + 1);
                params.put(i, new OurParameters(bits, statSec, P, Q, M, mults.get(i)));
            }
            return params;
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
    public static BigInteger prime(int bits, Random rand) {
        BigInteger cand;
        do {
            cand = BigInteger.probablePrime(bits, rand);
        } while (!cand.mod(BigInteger.valueOf(4)).equals(BigInteger.valueOf(3)));
        return cand;
    }

    public static Map<Integer, BigInteger> share(BigInteger value, int parties, BigInteger modulus, Random rand) {
        Map<Integer, BigInteger> shares = new ConcurrentHashMap<>(parties);
        BigInteger sum = BigInteger.ZERO;
        for (int i = 1; i < parties; i++) {
            BigInteger randomNumber = new BigInteger(modulus.bitLength(), rand);
            sum = sum.add(randomNumber);
            shares.put(i, randomNumber);
        }
        // Compute pivot
        shares.put(0, value.subtract(sum).mod(modulus));
        return shares;
    }
}