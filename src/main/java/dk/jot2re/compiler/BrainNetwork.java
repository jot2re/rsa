package dk.jot2re.compiler;

import dk.jot2re.network.INetwork;

import java.io.Serializable;

public class BrainNetwork extends PinkyNetwork {

    public BrainNetwork(INetwork internalNetwork) {
        super(internalNetwork);
    }

    @Override
    public void send(int recipientId, Serializable data) {
        digest.update(serializer.serialize(data));
        internalNetwork.send(getRecipientBrainId(recipientId), data);
        internalNetwork.send(getRecipientPinkyId(recipientId), data);
    }
}
