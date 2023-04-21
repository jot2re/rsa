package dk.jot2re.compiler;

import dk.jot2re.network.INetwork;
import dk.jot2re.network.Serializer;

import java.io.Serializable;
import java.security.MessageDigest;
import java.util.List;

import static dk.jot2re.DefaultSecParameters.getDigest;

public class PinkyNetwork implements INetwork {
    protected final MessageDigest digest = getDigest();
    protected final INetwork internalNetwork;
    protected final Serializer serializer;

    public PinkyNetwork(INetwork internalNetwork) {
        this.internalNetwork = internalNetwork;
        this.serializer = new Serializer();
    }

    @Override
    public void init() {
        internalNetwork.init();
    }

    @Override
    public void send(int recipientId, Serializable data) {
        digest.update(serializer.serialize(data));
    }

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
    public int myId() {
        return internalNetwork.myId();
    }

    public static int getSubmissivePinkyId(INetwork network) {
        return (network.myId()-1) % network.getNoOfParties();
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
        return (virtualId-1) % internalNetwork.getNoOfParties();
    }
}
