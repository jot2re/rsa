package dk.jot2re.mult;

import dk.jot2re.mult.ot.OTMultResourcePool;
import dk.jot2re.mult.ot.gilboa.GilboaMult;
import dk.jot2re.mult.ot.ips.IPSMult;
import dk.jot2re.mult.ot.ot.base.DummyOt;
import dk.jot2re.mult.ot.ot.base.Ot;
import dk.jot2re.mult.ot.ot.otextension.OtExtensionDummyContext;
import dk.jot2re.mult.ot.ot.otextension.OtExtensionResourcePool;
import dk.jot2re.mult.ot.util.AesCtrDrbg;
import dk.jot2re.mult.ot.util.Drbg;
import dk.jot2re.mult.ot.util.Drng;
import dk.jot2re.mult.ot.util.DrngImpl;
import dk.jot2re.mult.replicated.ReplicatedMult;
import dk.jot2re.mult.replicated.ReplictedMultResourcePool;
import dk.jot2re.mult.shamir.ShamirMult;
import dk.jot2re.mult.shamir.ShamirResourcePool;
import dk.jot2re.network.INetwork;
import dk.jot2re.network.NetworkFactory;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

import static dk.jot2re.DefaultSecParameters.*;

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

    // todo make working with the different kinds of mults
    public Map<Integer, IMult> getMults(MultType multType, NetworkFactory.NetworkType networkType) {
        Map<Integer, IMult> mults = new HashMap<>(parties);
        Map<Integer, INetwork> networks = networkFactory.getNetworks(networkType);
        for (int i = 0; i < parties; i++) {
            if (multType == MultType.DUMMY) {
                mults.put(i, new DummyMult());
            } else if (multType == MultType.REPLICATED) {
                ReplictedMultResourcePool resourcePool = new ReplictedMultResourcePool(i, parties, comSec, statSec, getDrng(i));
                mults.put(i, new ReplicatedMult(resourcePool));
            } else if (multType == MultType.SHAMIR) {
                ShamirResourcePool resourcePool = new ShamirResourcePool(i, parties, comSec, statSec, getDrng(i));
                mults.put(i, new ShamirMult(resourcePool));
            } else if (multType == MultType.GILBOA) {
                if (parties > 2) {
                    throw new IllegalArgumentException("Gilboa for more than 2 parties not implemented");
                }
                try {
                    return getOTMult(networks, MultType.GILBOA, masterSeed, OT_SAFE_EXPANSION);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else if (multType == MultType.IPS) {
                try {
                    return getOTMult(networks, MultType.IPS, masterSeed, OT_SAFE_EXPANSION);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else {
                throw new IllegalArgumentException("Unsupported multiplication type");
            }
            mults.get(i).init(networks.get(i));
        }
        return mults;
    }

    // TODO refactor into a single, simple iterative method with the other types of mults
    private Map<Integer, IMult> getOTMult(Map<Integer, INetwork> networks, MultType type, byte[] masterSeed, boolean safeExpansion) throws InterruptedException, ExecutionException {
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
                    mult = new IPSMult(pool, 32 * MODULO_BITLENGTH, safeExpansion);
                } else if (type.equals(MultType.GILBOA)) {
                    OtExtensionResourcePool pool = (new OtExtensionDummyContext(finalI,1-finalI, comSec, statSec, seed, networks.get(finalI))).createResources(0);
                    mult = new GilboaMult(pool, 32*MODULO_BITLENGTH, safeExpansion);
                }
                // the constant mult be a 2-power larger than the amount of parties
                mult.init(networks.get(finalI));
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
}
