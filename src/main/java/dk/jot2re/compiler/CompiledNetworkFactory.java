package dk.jot2re.compiler;

import dk.jot2re.network.INetwork;
import dk.jot2re.network.NetworkFactory;

import java.util.HashMap;
import java.util.Map;

public class CompiledNetworkFactory {
    private final NetworkFactory internalFactory;
    public CompiledNetworkFactory(NetworkFactory internalFactory) {
        this.internalFactory = internalFactory;
    }

    public Map<Integer, NetworkPair> getNetworks(NetworkFactory.NetworkType type) {
        Map<Integer, BrainNetwork> baseBrainNetworks = internalFactory.getNetworks(type);
        Map<Integer, PinkyNetwork> basePinkyNetworks = internalFactory.getNetworks(type);
        Map<Integer, NetworkPair> resNetworks = new HashMap<>(internalFactory.getParties());
        for (int i = 0; i <internalFactory.getParties(); i++) {
            INetwork pinkyNet = basePinkyNetworks.get(BaseNetwork.getMyVirtualPinkyId(baseBrainNetworks.get(i)));
            BrainNetwork brainNetwork = new BrainNetwork(baseBrainNetworks.get(i), basePinkyNetworks.get(i));
            PinkyNetwork pinkyNetwork = new PinkyNetwork(pinkyNet);
            resNetworks.put(i, new NetworkPair(brainNetwork, pinkyNetwork));
        }
        return resNetworks;
    }
}
