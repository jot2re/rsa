package anonymous.compiler;

import anonymous.network.INetwork;
import anonymous.network.Serializer;

import java.io.Serializable;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static anonymous.DefaultSecParameters.getDigest;

public abstract class BaseNetwork implements INetwork {
    protected Map<Integer, MessageDigest> sndDigests;
    public final INetwork internalNetwork;
    protected final Serializer serializer;

    public BaseNetwork(INetwork internalNetwork) {
        this.internalNetwork = internalNetwork;
        this.serializer = new Serializer();
        this.sndDigests = new HashMap<>(internalNetwork.getNoOfParties());
        for (int i = 0; i < internalNetwork.getNoOfParties(); i++) {
            sndDigests.put(i, getDigest());
        }
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


    public byte[] getDigestSnd(int party) {
        return sndDigests.get(party).digest();
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
