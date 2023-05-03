package anonymous.compiler;

import anonymous.network.INetwork;
import anonymous.network.Serializer;

import java.io.Serializable;
import java.security.MessageDigest;
import java.util.List;

import static anonymous.DefaultSecParameters.getDigest;

public abstract class BaseNetwork implements INetwork {
    protected final MessageDigest digest = getDigest();
    protected final INetwork internalNetwork;
    protected final Serializer serializer;

    public BaseNetwork(INetwork internalNetwork) {
        this.internalNetwork = internalNetwork;
        this.serializer = new Serializer();
    }

    @Override
    public void init() {
        internalNetwork.init();
    }

    @Override
    public abstract void send(int recipientId, Serializable data);


    @Override
    public <T extends Serializable> T receive(int senderId) {
        if (senderId == myId()) {
//            throw new RuntimeException("Should not happen");
            return null;
        }
        T brainRes = internalNetwork.receive(senderId);
        byte[] serializedData = serializer.serialize(brainRes);
        digest.update(serializedData);
        return brainRes;
    }

    @Override
    public int getNoOfParties() {
        return internalNetwork.getNoOfParties();
    }

    @Override
    public List<Integer> peers() {
        return internalNetwork.peers();
    }

    @Override
    public int myId() {
        return internalNetwork.myId();
    }

    public byte[] getDigestRes() {
        return digest.digest();
    }

    public static int getSubmissivePinkyId(INetwork network) {
        if (network.myId() -1 >= 0) {
            return network.myId() -1;
        } else {
            return network.myId() -1+network.getNoOfParties();
        }
    }

    public static int getMyVirtualPinkyId(INetwork network) {
        return (network.myId()+1) % network.getNoOfParties();
    }

    public static int getSubmissivePinkyId(int id, int parties) {
        if (id -1 >= 0) {
            return id -1;
        } else {
            return id -1+parties;
        }
    }

    public static int getMyVirtualPinkyId(int id, int parties) {
        return (id+1) % parties;
    }
}
