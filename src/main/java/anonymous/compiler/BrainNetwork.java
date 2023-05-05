package anonymous.compiler;

import anonymous.network.INetwork;
import anonymous.network.NetworkException;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class BrainNetwork extends BaseNetwork {
    private final INetwork pinkyNetwork;

    public BrainNetwork(INetwork brainNetwork, INetwork pinkyNetwork) {
        super(brainNetwork);
        this.pinkyNetwork = pinkyNetwork;
    }

    @Override
    public void send(int recipientId, Serializable data) {
        // We don't want to send data to ourselves, but only store it
        internalNetwork.send(recipientId, data);
        pinkyNetwork.send(recipientId, data);
        sndDigests.get(recipientId).update(serializer.serialize(data));
    }

    @Override
    public void sendToAll(Serializable data) throws NetworkException {
        for (int i = 0; i < internalNetwork.getNoOfParties(); i++) {
            if (i != internalNetwork.myId()) {
                internalNetwork.send(i, data);
            }
            if (i != pinkyNetwork.myId()) {
                pinkyNetwork.send(i, data);
            }
        }
    }

    @Override
    public  <T extends Serializable> Map<Integer, T> receiveFromAllPeers() {
        Map<Integer, T> values = new HashMap<>(getNoOfParties());
        for (int i = 0; i < internalNetwork.getNoOfParties(); i++) {
            if (i != myId()) {
                values.put(i, receive(i));
            }
        }
        return values;
    }
}
