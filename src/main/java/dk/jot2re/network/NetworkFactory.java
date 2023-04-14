package dk.jot2re.network;

import java.util.HashMap;
import java.util.Map;

public class NetworkFactory {
    public enum NetworkType {
        DUMMY,
        PLAIN
    }

    private final int parties;
    public NetworkFactory(int parties) {
        this.parties = parties;
    }

    public <T extends INetwork> Map<Integer, T> getNetworks(NetworkType type) {
        Map<Integer, T> map = new HashMap<>(parties);
        if (type == NetworkType.DUMMY) {
            DummyState state = new DummyState(parties);
            for (int i = 0; i < parties; i++) {
                T network = (T) new DummyNetwork(state, i);
                network.init();
                map.put(i, network);
            }
        } else if (type == NetworkType.PLAIN) {
            for (int i = 0; i < parties; i++) {
                T network = (T) new PlainNetwork<>(i, parties, 0, null);
                network.init();
                map.put(i, network);
            }
        } else {
            throw new IllegalArgumentException("Unknown network type");
        }
        return map;
    }
}
