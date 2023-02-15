package dk.jot2re.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class DummyP2P implements IP2P {
    private static final Logger logger = LoggerFactory.getLogger(DummyP2P.class);
    private final DummyState state;
    private final int myId;
    private final int peerId;
    private final ByteArrayOutputStream byteStream;
    private final ObjectOutputStream writer;
    private long bytesSent = 0L;
    private int transfers = 0;

    public DummyP2P(DummyState state, int myId, int peerId) {
        this.state = state;
        this.myId = myId;
        this.peerId = peerId;
        try {
            byteStream = new ByteArrayOutputStream();
            writer = new ObjectOutputStream(byteStream);
        } catch (Exception e) {
            throw new RuntimeException("Could not initialize p2p network", e);
        }
    }
    @Override
    public void init() {
    }

    @Override
    public void send(Serializable data) throws NetworkException {
        try {
            writer.writeObject(data);
            writer.flush();
            logger.debug("Sending " + byteStream.size() + " bytes from party " + myId + " to party " + peerId);
            bytesSent += byteStream.size();
            transfers++;
            byteStream.reset();
            writer.reset();

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

    public long getBytesSent() {
        return bytesSent;
    }

    public int getTransfers() {
        return transfers;
    }
}
