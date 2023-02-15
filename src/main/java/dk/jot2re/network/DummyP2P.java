package dk.jot2re.network;

import java.io.Serializable;

public class DummyP2P implements IP2P {
    private final DummyState state;
    private final int myId;
    private final int peerId;
    public DummyP2P(DummyState state, int myId, int peerId) {
        this.state = state;
        this.myId = myId;
        this.peerId = peerId;
    }
    @Override
    public void init() {

    }

    @Override
    public void send(Serializable data) throws NetworkException {
        try {
            state.put(myId, peerId, data);
        } catch (Exception e) {
            throw new NetworkException(e.getMessage(), e);
        }
    }

    @Override
    public Serializable receive() {
        return state.get(myId, peerId);
    }

    @Override
    public int myId() {
        return myId;
    }

    @Override
    public int peerId() {
        return peerId;
    }
}
