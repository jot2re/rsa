package dk.jot2re.compiler;

import dk.jot2re.network.INetwork;
import dk.jot2re.network.NetworkException;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class PinkyNetwork extends BaseNetwork {

    public PinkyNetwork(INetwork internalNetwork) {
        super(internalNetwork);
    }

    @Override
    public void send(int recipientId, Serializable data) {
        //todo some limit on what should be stored
        digest.update(serializer.serialize(data));
    }

    @Override
    public void sendToAll(Serializable data) throws NetworkException {
        for (int i = 0; i < internalNetwork.getNoOfParties(); i++) {
            if (i != myId()) {
                send(i, data);
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
