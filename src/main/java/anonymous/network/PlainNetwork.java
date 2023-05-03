package anonymous.network;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlainNetwork<T extends Serializable> implements INetwork {
    private final int myId;
    private final int pivotId;
    private final int parties;
    private final List<Integer> peers;
    private final List<Map<Integer, T>> resultList;
    private int counter = 0;
    private T defaultResponse = (T) BigInteger.ZERO;

    public PlainNetwork(int myId, int parties, int pivotId, List<Map<Integer, T>> resultList) {
        this.myId = myId;
        this.parties = parties;
        this.pivotId = pivotId;
        this.peers = new ArrayList<>();
        this.resultList = resultList;
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
    public void send(int recipientId, Serializable data) {

    }

    @Override
    public T receive(int senderId) {
        if (resultList != null && resultList.get(counter) != null) {
            return resultList.get(counter++).get(senderId);
        } else {
            return defaultResponse;
        }
    }

    @Override
    public void sendToAll(Serializable data) throws NetworkException {

    }

    @Override
    public int getNoOfParties() {
        return parties;
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
    public Map<Integer, T> receiveFromAllPeers() {
        if (resultList != null && resultList.get(counter) != null) {
            return resultList.get(counter++);
        } else {
            Map res = new HashMap(peers.size());
            for (int i : peers) {
                res.put(i, defaultResponse);
            }
            return res;
        }
    }

    @Override
    public Map<Integer, T> receiveFromNonPivotPeers() {
        if (resultList != null && resultList.get(counter) != null) {
            return resultList.get(counter++);
        } else {
            Map res = new HashMap(peers.size());
            for (int i : peers) {
                if (i != pivotId) {
                    res.put(i, defaultResponse);
                }
            }
            return res;
        }
    }

    public void setDefaultResponse(T response) {
        this.defaultResponse = response;
    }
}
