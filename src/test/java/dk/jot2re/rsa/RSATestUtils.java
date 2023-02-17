package dk.jot2re.rsa;

import dk.jot2re.mult.DummyMultFactory;
import dk.jot2re.mult.IMult;
import dk.jot2re.network.DummyNetwork;
import dk.jot2re.network.DummyNetworkFactory;
import dk.jot2re.rsa.bf.BFParameters;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class RSATestUtils {
    public static Map<Integer, BFParameters> getParameters(int bits, int statSec, int parties) throws Exception {
        DummyNetworkFactory netFactory = new DummyNetworkFactory(parties);
        DummyMultFactory multFactory = new DummyMultFactory(parties);
        Map<Integer, DummyNetwork> networks = netFactory.getNetworks();
        Map<Integer, BFParameters> params = new HashMap<>(parties);
        Map<Integer, IMult> mults = multFactory.getMults();
        for (int i = 0; i < networks.size(); i++) {
            // Unique but deterministic seed for each set of parameters
            SecureRandom rand = SecureRandom.getInstance("SHA1PRNG", "SUN");
            // Note that seed is only updated if different from 0
            rand.setSeed(networks.get(i).myId()+1);
            params.put(networks.get(i).myId(), new BFParameters(bits, statSec, networks.get(i), mults.get(i), rand));
        }
        return params;
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
}
