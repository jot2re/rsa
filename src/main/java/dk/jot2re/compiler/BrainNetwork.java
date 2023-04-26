package dk.jot2re.compiler;

import dk.jot2re.network.INetwork;

import java.io.Serializable;

public class BrainNetwork extends CompiledNetwork {
    private final INetwork pinkyNetwork;

    public BrainNetwork(INetwork brainNetwork, INetwork pinkyNetwork) {
        super(brainNetwork);
        this.pinkyNetwork = pinkyNetwork;
    }

    @Override
    public void send(int recipientId, Serializable data) {
        // We don't want to send data to ourselves, but only store it
//        if (getRecipientBrainId(recipientId) != myId()) {
            internalNetwork.send(getRecipientBrainId(recipientId), data);
//        }
//        if (getRecipientPinkyId(recipientId) != myId()) {
            pinkyNetwork.send(getRecipientPinkyId(recipientId), data);
//        }
        digest.update(serializer.serialize(data));
    }

    @Override
    public int myId() {
        return getMyVirtualBrainId(internalNetwork);
    }
}
