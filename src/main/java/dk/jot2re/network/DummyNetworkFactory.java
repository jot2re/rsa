package dk.jot2re.network;

import java.util.HashMap;
import java.util.Map;

public class DummyNetworkFactory {
    private final int parties;
    public DummyNetworkFactory(int parties) {
        this.parties = parties;
    }

    public Map<Integer, DummyNetwork> getNetworks() {
        DummyState state = new DummyState(parties);
        Map<Integer, DummyNetwork> map = new HashMap<>(parties);
        for (int i = 0; i < parties; i++) {
            DummyNetwork network = new DummyNetwork(state, i);
            network.init();
            map.put(i, network);
        }
        return map;
    }
}
