package dk.jot2re.compiler;

import dk.jot2re.network.INetwork;

import java.io.Serializable;

public class PinkyNetwork extends CompiledNetwork {

    public PinkyNetwork(INetwork internalNetwork) {
        super(internalNetwork);
    }

    @Override
    public void send(int recipientId, Serializable data) {
        digest.update(serializer.serialize(data));
    }

    @Override
    public int myId() {
        return getMyVirtualPinkyId(internalNetwork);
    }
}
