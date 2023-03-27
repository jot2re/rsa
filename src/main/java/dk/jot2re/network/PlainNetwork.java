package dk.jot2re.network;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlainNetwork implements INetwork {
    private final int myId;
    private final int pivotId;
    private final int parties;
    private final List<Integer> peers;
    private BigInteger defaultResponse = BigInteger.ZERO;

    public PlainNetwork(int myId, int parties, int pivotId) {
        this.myId = myId;
        this.parties = parties;
        this.pivotId = pivotId;
        this.peers = new ArrayList<>();
        for (int i = 0; i < parties; i++) {
            if (i != myId) {
                peers.add(i);
            }
        }
    }

    @Override
    public void init() {

    }

    @Override
    public void send(int recipientId, Serializable data) throws NetworkException {

    }

    @Override
    public <T extends Serializable> T receive(int senderId) {
        return (T) BigInteger.ZERO;
    }

    @Override
    public void sendToAll(Serializable data) throws NetworkException {

    }

    @Override
    public List<Integer> peers() {
        return peers;
    }

    @Override
    public int myId() {
        return myId;
    }

    @Override
    public <T extends Serializable> Map<Integer, T> receiveFromAllPeers() {
        Map res = new HashMap(peers.size());
        for (int i: peers) {
            res.put(i, defaultResponse);
        }
        return res;
    }

    @Override
    public <T extends Serializable> Map<Integer, T> receiveFromNonPivotPeers() {
        Map res = new HashMap(peers.size());
        for (int i: peers) {
            if (i != pivotId) {
                res.put(i, defaultResponse);
            }
        }
        return res;
    }

    public void setDefaultResponse(BigInteger response) {
        this.defaultResponse = response;
    }
}
