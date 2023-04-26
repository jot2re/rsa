package dk.jot2re.compiler;

import dk.jot2re.network.INetwork;
import dk.jot2re.network.Serializer;

import java.io.Serializable;
import java.security.MessageDigest;
import java.util.List;

import static dk.jot2re.DefaultSecParameters.getDigest;

public abstract class CompiledNetwork implements INetwork {
    protected final MessageDigest digest = getDigest();
    protected final INetwork internalNetwork;
    protected final Serializer serializer;

    public CompiledNetwork(INetwork internalNetwork) {
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
        T brainRes = internalNetwork.receive(getRecipientBrainId(senderId));
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
    public abstract int myId();

    public static int getSubmissivePinkyId(INetwork network) {
        if (network.myId() -1 >= 0) {
            return network.myId() -1;
        } else {
            return network.myId() -1+network.getNoOfParties();
        }
    }

    public static int getMyVirtualBrainId(INetwork network) {
        return network.myId();
    }

    public static int getMyVirtualPinkyId(INetwork network) {
        return (network.myId()+1) % network.getNoOfParties();
    }

    protected int getRecipientBrainId(int virtualId) {
        return virtualId;
    }

    protected int getRecipientPinkyId(int virtualId) {
        if (virtualId -1 >= 0) {
            return virtualId -1;
        } else {
            return virtualId -1+internalNetwork.getNoOfParties();
        }
    }
}
