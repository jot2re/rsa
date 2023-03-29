package dk.jot2re.mult;

import dk.jot2re.network.DummyNetworkFactory;
import dk.jot2re.network.INetwork;

import java.util.HashMap;
import java.util.Map;

public class DummyMultFactory {
    private final int parties;
    private final DummyNetworkFactory networkFactory;
    public DummyMultFactory(int parties) {
        this.parties = parties;
        this.networkFactory = new DummyNetworkFactory(parties);
    }

    public Map<Integer, IMult> getMults() {
        Map<Integer, IMult> mults = new HashMap<>(parties);
        Map<Integer, INetwork> networks = networkFactory.getNetworks();
        for (int i = 0; i < parties; i++) {
            mults.put(i, new DummyMult());
            mults.get(i).init(networks.get(i));
        }
        return mults;
    }
}
