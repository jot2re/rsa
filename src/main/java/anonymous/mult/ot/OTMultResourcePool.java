package anonymous.mult.ot;

import anonymous.mult.ot.cointossing.CoinTossing;
import anonymous.mult.ot.ot.base.Ot;
import anonymous.mult.ot.ot.otextension.OtExtensionResourcePool;
import anonymous.mult.ot.ot.otextension.OtExtensionResourcePoolImpl;
import anonymous.mult.ot.ot.otextension.RotList;
import anonymous.mult.ot.util.Drbg;
import anonymous.network.INetwork;

import java.util.HashMap;
import java.util.Map;

public class OTMultResourcePool {
    // todo refactor into a basic resource containing the variables we always need
    private final int myId;
    private final int parties;
    private final int compSec;
    private final int statSec;
    private final Map<Integer, OtExtensionResourcePool> otResources;

    public OTMultResourcePool(Map<Integer, Ot> ots, int myId, int compSec, int statSec, INetwork network, Drbg drbg) {
        this(myId, compSec, statSec, network, drbg, makeSeedOts(myId, compSec, drbg, ots));
    }

    private static Map<Integer, RotList> makeSeedOts(int myId, int compSec, Drbg drbg, Map<Integer, Ot> ots) {
        Map<Integer, RotList> res = new HashMap<>(ots.size());
        for (int i = 0; i < ots.size()+1; i++) {
            if (i != myId) {
                RotList seedOts = new RotList(drbg, compSec);
                if (myId < i) {
                    seedOts.send(ots.get(i));
                    seedOts.receive(ots.get(i));
                } else {
                    seedOts.receive(ots.get(i));
                    seedOts.send(ots.get(i));
                }
                res.put(i, seedOts);
            }
        }
        return res;
    }

    public OTMultResourcePool(int myId, int compSec, int statSec, INetwork network, Drbg drbg, Map<Integer, RotList> seedOts) {
        this.myId = myId;
        this.parties = seedOts.size()+1;
        this.compSec = compSec;
        this.statSec = statSec;
        this.otResources = new HashMap<>(parties-1);
        for (int i: seedOts.keySet()) {
            CoinTossing ct = new CoinTossing(myId, i, drbg);
            ct.initialize(network);
            OtExtensionResourcePool curResources = new OtExtensionResourcePoolImpl(myId, i, compSec, statSec, 0, drbg, network, ct, seedOts.get(i));
            otResources.put(i, curResources);
        }
    }

    public int getMyId() {
        return myId;
    }

    public int getParties() {
        return parties;
    }

    public int getCompSec() {
        return compSec;
    }

    public int getStatSec() {
        return statSec;
    }

    public OtExtensionResourcePool getOtResources(int otherId) {
        return otResources.get(otherId);
    }
}
