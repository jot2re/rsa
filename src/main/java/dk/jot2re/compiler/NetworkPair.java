package dk.jot2re.compiler;

public class NetworkPair {
    private final BrainNetwork brainNetwork;
    private final PinkyNetwork pinkyNetwork;

    public NetworkPair(BrainNetwork brainNetwork, PinkyNetwork pinkyNetwork) {
        this.brainNetwork = brainNetwork;
        this.pinkyNetwork = pinkyNetwork;
    }

    public BrainNetwork getBrainNetwork() {
        return brainNetwork;
    }

    public PinkyNetwork getPinkyNetwork() {
        return pinkyNetwork;
    }
}
