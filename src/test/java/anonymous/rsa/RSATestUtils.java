package anonymous.rsa;

import anonymous.mult.IMult;
import anonymous.mult.MultFactory;
import anonymous.mult.ot.util.ExceptionConverter;
import anonymous.network.INetwork;
import anonymous.network.NetworkFactory;
import anonymous.rsa.bf.BFParameters;
import anonymous.rsa.our.OurParameters;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class RSATestUtils {
    public static final MultFactory.MultType MULT_TYPE = MultFactory.MultType.DUMMY;
    public static final NetworkFactory.NetworkType NETWORK_TYPE = NetworkFactory.NetworkType.DUMMY;

    public static Map<Integer, BigInteger> randomPrime(int parties, int bitLength, Random rand) {
        BigInteger prime = RSATestUtils.prime(bitLength, rand);
        Map<Integer, BigInteger> shares = new HashMap<>(parties);
        // We sample a number small enough to avoid issues with negative shares
        for (int party = 1; party < parties; party++) {
            shares.put(party, (new BigInteger(bitLength - 4 - parties, rand)).multiply(BigInteger.valueOf(4)));
        }
        shares.put(0, prime.subtract(shares.values().stream().reduce(BigInteger.ZERO, (a, b)-> a.add(b))));
        return shares;
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

    public static Map<Integer, INetwork> getNetworks(int parties) {
        NetworkFactory netFactory = new NetworkFactory(parties);
        return netFactory.getNetworks(NETWORK_TYPE);
    }

    public static Map<Integer, BFParameters> getBFParameters(int bits, int statSec, int parties) {
        try {
            MultFactory multFactory = new MultFactory(parties);
            Map<Integer, INetwork> networks = getNetworks(parties);
            Map<Integer, BFParameters> params = new HashMap<>(parties);
            Map<Integer, IMult> mults = multFactory.getMults(MULT_TYPE, NETWORK_TYPE, true);
            for (int i = 0; i < networks.size(); i++) {
                // Unique but deterministic seed for each set of parameters
                SecureRandom rand = SecureRandom.getInstance("SHA1PRNG", "SUN");
                // Note that seed is only updated if different from 0
                rand.setSeed(networks.get(i).myId() + 1);
                params.put(networks.get(i).myId(), new BFParameters(bits, statSec, mults.get(i)));
            }
            return params;
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public static Map<Integer, OurParameters> getOurParameters(int bits, int statSec, int parties, boolean decorated) {
        return getOurParameters(bits, statSec, parties, decorated, MultFactory.MultType.DUMMY);
    }

    public static Map<Integer, OurParameters> getOurParameters(int bits, int statSec, int parties) {
        return getOurParameters(bits, statSec, parties, false, MultFactory.MultType.DUMMY);
    }

    public static Map<Integer, OurParameters> getOurParameters(int bits, int statSec, int parties, boolean decorated, MultFactory.MultType multType) {
        try {
            // TODO the 8 increments are needed for OT mult protocols but not others
            // M > 2^(2*bits)
            BigInteger M = RSATestUtils.prime(2*bits+8, new Random(42));
            // P > mN, we assume at most 2048 parties
            BigInteger P = RSATestUtils.prime(2*bits+16, new Random(42));
            // Q > P
            BigInteger Q = RSATestUtils.prime(2*bits+24, new Random(42));
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

    public static Map<Integer, BigInteger> randomSharing(BigInteger modulo, int parties, Random rand) {
        Map<Integer, BigInteger> res = new HashMap<>(parties);
        for (int i = 0; i < parties; i++) {
            res.put(i, new BigInteger(modulo.bitLength(), rand));
        }
        return res;
    }

    public static Random getRandom(int myId) {
        SecureRandom random = ExceptionConverter.safe( ()-> SecureRandom.getInstance("SHA1PRNG", "SUN"), "Could not get random");
        random.setSeed(myId);
        return random;
    }
}