package dk.jot2re.mult;

import dk.jot2re.network.NetworkFactory;
import dk.jot2re.network.INetwork;

import java.util.HashMap;
import java.util.Map;

public class MultFactory {
    public enum MultType {
        DUMMY,
        OT,
        GILBOA,
        IPS,
        REPLICATED,
        SHAMIT
    }
    
    private final int parties;
    private final NetworkFactory networkFactory;
    public MultFactory(int parties) {
        this.parties = parties;
        this.networkFactory = new NetworkFactory(parties);
    }

    public MultFactory(int parties, int comSec, int statSec) {
        this.parties = parties;
        this.networkFactory = new NetworkFactory(parties);
    }

    // todo make working with the different kinds of mults
    public Map<Integer, IMult> getMults(MultType multType, NetworkFactory.NetworkType networkType) {
        Map<Integer, IMult> mults = new HashMap<>(parties);
        Map<Integer, INetwork> networks = networkFactory.getNetworks(networkType);
        for (int i = 0; i < parties; i++) {
            switch (multType) {
                case DUMMY:
                    mults.put(i, new DummyMult());
                    break;
                case REPLICATED:
                    mults.put(i, new DummyMult());
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported multiplication type");
            }
            mults.put(i, new DummyMult());
            mults.get(i).init(networks.get(i));
        }
        return mults;
    }
}
