package anonymous.mult;

import anonymous.mult.ot.OTMultResourcePool;
import anonymous.mult.ot.gilboa.GilboaMult;
import anonymous.mult.ot.ips.IPSMult;
import anonymous.mult.ot.ot.base.DummyOt;
import anonymous.mult.ot.ot.base.Ot;
import anonymous.mult.ot.ot.otextension.OtExtensionDummyContext;
import anonymous.mult.ot.ot.otextension.OtExtensionResourcePool;
import anonymous.mult.ot.util.*;
import anonymous.mult.replicated.ReplicatedMult;
import anonymous.mult.replicated.ReplictedMultResourcePool;
import anonymous.mult.shamir.ShamirMult;
import anonymous.mult.shamir.ShamirResourcePool;
import anonymous.network.INetwork;
import anonymous.network.NetworkFactory;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.*;

import static anonymous.DefaultSecParameters.*;

public class MultFactory {
    public enum MultType {
        DUMMY,
        GILBOA,
        IPS,
        REPLICATED,
        SHAMIR
    }
    public static final boolean OT_SAFE_EXPANSION = false;
    
    private final int parties;
    private final int comSec;
    private final int statSec;
    private final byte[] masterSeed;
    private final NetworkFactory networkFactory;
    public MultFactory(int parties) {
        this.parties = parties;
        this.networkFactory = new NetworkFactory(parties);
        this.comSec = COMP_SEC;
        this.statSec = STAT_SEC;
        this.masterSeed = new SecureRandom().generateSeed(comSec/8);
    }

    public MultFactory(int parties, int comSec, int statSec, byte[] masterSeed) {
        this.parties = parties;
        this.networkFactory = new NetworkFactory(parties);
        this.comSec = comSec;
        this.statSec = statSec;
        this.masterSeed = masterSeed;
    }

    public Map<Integer, IMult> getMults(MultType multType, NetworkFactory.NetworkType networkType) {
        return getMults(multType, networkType, false);
    }

    public Map<Integer, IMult> getMults(MultType multType, NetworkFactory.NetworkType networkType, boolean decorated) {
        Map<Integer, IMult> mults = new HashMap<>(parties);
        for (int i = 0; i < parties; i++) {
            IMult cur;
            if (multType == MultType.DUMMY) {
                cur = decorated ? new MultCounter(new DummyMult()) : new DummyMult();
            } else if (multType == MultType.REPLICATED) {
                ReplictedMultResourcePool resourcePool = new ReplictedMultResourcePool(i, parties, comSec, statSec);
                cur = decorated ? new MultCounter(new ReplicatedMult(resourcePool)) : new ReplicatedMult(resourcePool);
            } else if (multType == MultType.SHAMIR) {
                ShamirResourcePool resourcePool = new ShamirResourcePool(i, parties, comSec, statSec);
                cur = decorated ? new MultCounter(new ShamirMult(resourcePool)) : new ShamirMult(resourcePool);
            } else if (multType == MultType.GILBOA) {
                if (parties > 2) {
                    throw new IllegalArgumentException("Gilboa for more than 2 parties not implemented");
                }
                try {
                    Map<Integer, INetwork> networks = networkFactory.getNetworks(networkType);
                    return getOTMult(networks, MultType.GILBOA, masterSeed, OT_SAFE_EXPANSION, decorated);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else if (multType == MultType.IPS) {
                try {
                    Map<Integer, INetwork> networks = networkFactory.getNetworks(networkType);
                    return getOTMult(networks, MultType.IPS, masterSeed, OT_SAFE_EXPANSION, decorated);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else {
                throw new IllegalArgumentException("Unsupported multiplication type");
            }
            mults.put(i, cur);
        }
        return mults;
    }

    // TODO refactor into a single, simple iterative method with the other types of mults
    private Map<Integer, IMult> getOTMult(Map<Integer, INetwork> networks, MultType type, byte[] masterSeed, boolean safeExpansion, boolean decorated) throws InterruptedException, ExecutionException {
        Map<Integer, Future<IMult>> mults = new HashMap<>(parties);
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        for (int i = 0; i < parties; i++) {
            int finalI = i;
            Future<IMult> curMult = executor.submit(() -> {
                Map<Integer, Ot> ots = new HashMap<>(parties-1);
                for (int j = 0; j < parties; j++) {
                    if (finalI != j) {
                        ots.put(j, new DummyOt(j, networks.get(finalI)));
                    }
                }
                byte[] seed = masterSeed.clone();
                seed[0] = (byte) finalI;
                Drbg rand = new AesCtrDrbg(seed);
                IMult mult = null;
                if (type.equals(MultType.IPS)) {
                    OTMultResourcePool pool = new OTMultResourcePool(ots, finalI, comSec, statSec, networks.get(finalI), rand);
                    mult = decorated ? new MultCounter(new IPSMult(pool, 32 * MODULO_BITLENGTH, safeExpansion)) : new IPSMult(pool, 32 * MODULO_BITLENGTH, safeExpansion);
                } else if (type.equals(MultType.GILBOA)) {
                    OtExtensionResourcePool pool = (new OtExtensionDummyContext(finalI,1-finalI, comSec, statSec, seed, networks.get(finalI))).createResources(0);
                    mult = decorated ? new MultCounter(new GilboaMult(pool, 32 * MODULO_BITLENGTH, safeExpansion)) : new GilboaMult(pool, 32 * MODULO_BITLENGTH, safeExpansion);
                }
                // the constant mult be a 2-power larger than the amount of parties
                mult.init(networks.get(finalI), getRandom(finalI));
                return mult;
            });
            mults.put(i, curMult);
        }
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.SECONDS);
        Map<Integer, IMult> res = new HashMap<>(parties);
        for (int i: mults.keySet()) {
            res.put(i, mults.get(i).get());
        }
        return res;
    }

    private Drng getDrng(int myId) {
        byte[] seed = masterSeed.clone();
        seed[0] ^= (byte) myId;
        return new DrngImpl(new AesCtrDrbg(seed));
    }

    private Random getRandom(int myId) {
        SecureRandom random = ExceptionConverter.safe( ()-> SecureRandom.getInstance("SHA1PRNG", "SUN"), "Could not get random");
        byte[] seed = masterSeed.clone();
        seed[0] ^= (byte) myId;
        random.setSeed(seed);
        return random;
    }
}
