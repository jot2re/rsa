package dk.jot2re.network;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class DummyNetwork implements INetwork {
    private final int myId;
    private final int peers;
    private final Map<Integer, DummyP2P> networks;
    public DummyNetwork(DummyState state, int myId) {
        this.myId = myId;
        this.peers = state.parties();
        this.networks = new HashMap<>(peers);
        for (int i = 0; i < peers; i++) {
            DummyP2P p2p = new DummyP2P(state, myId, i);
            p2p.init();
            networks.put(i, p2p);
        }
    }

    @Override
    public void init() {

    }

    @Override
    public void send(int recipientId, Serializable data) throws NetworkException {
        networks.get(recipientId).send(data);
    }

    @Override
    public Serializable receive(int senderId) {
        return networks.get(senderId).receive();
    }

    @Override
    public void sendToAll(Serializable data) throws NetworkException {
        for (IP2P network: networks.values()) {
            network.send(data);
        }
    }

    @Override
    public int peers() {
        return peers;
    }

    @Override
    public int myId() {
        return myId;
    }

    public long getBytesSent() {
        long bytes = 0L;
        for (DummyP2P network: networks.values()) {
            bytes += network.getBytesSent();
        }
        return bytes;
    }

    public long getTransfers() {
        long transfers = 0L;
        for (DummyP2P network: networks.values()) {
            transfers += network.getTransfers();
        }
        return transfers;
    }

}
