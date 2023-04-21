package dk.jot2re.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;

public class DummyP2P implements IP2P {
    private static final Logger logger = LoggerFactory.getLogger(DummyP2P.class);
    private final DummyState state;
    private final int myId;
    private final int peerId;
    private final Serializer serializer;
    private long bytesSent = 0L;
    private int transfers = 0;
    private int rounds = 0;
    private boolean lastOpSend = true;

    public DummyP2P(DummyState state, int myId, int peerId) {
        this.state = state;
        this.myId = myId;
        this.peerId = peerId;
        this.serializer = new Serializer();
    }
    @Override
    public void init() {
    }

    @Override
    public synchronized void send(Serializable data) {
        try {
            byte[] res = serializer.serialize(data);
            logger.debug("Sending " + res.length + " bytes from party " + myId + " to party " + peerId);
            bytesSent += res.length;
            transfers++;
            lastOpSend = true;
            state.put(myId, peerId, data);
        } catch (Exception e) {
            logger.error("ERROR: " +  e.getMessage());
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public synchronized Serializable receive() {
        Serializable res = state.get(peerId, myId);
        if (res != null && lastOpSend) {
            rounds++;
            lastOpSend = false;
        }
        return res;
    }

    @Override
    public int myId() {
        return myId;
    }

    @Override
    public int peerId() {
        return peerId;
    }

    public void resetCount() {
        bytesSent = 0;
        transfers = 0;
        rounds = 0;
    }

    public long getBytesSent() {
        return bytesSent;
    }

    public int getTransfers() {
        return transfers;
    }

    public int getRounds() {
        return rounds;
    }
}
