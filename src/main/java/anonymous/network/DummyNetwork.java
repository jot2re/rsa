package anonymous.network;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DummyNetwork implements INetwork {
    public static long TIME_OUT_MS  = 100000;
    public static final int WAIT_MS = 1;
    private final int myId;
    private final int parties;
    private final Map<Integer, DummyP2P> networks;
    private final List<Integer> peers;

    private long networkTime = 0;

    public DummyNetwork(DummyState state, int myId) {
        this.myId = myId;
        this.parties = state.parties();
        this.networks = new HashMap<>(parties);
        for (int i = 0; i < parties; i++) {
            if (i == myId) {
                continue;
            }
            DummyP2P p2p = new DummyP2P(state, myId, i);
            p2p.init();
            networks.put(i, p2p);
        }
        peers = networks.keySet().stream().toList();
    }

    @Override
    public void init() {

    }

    @Override
    public void send(int recipientId, Serializable data) {
        networks.get(recipientId).send(data);
    }

    @Override
    public Serializable receive(int senderId) {
        Serializable res = networks.get(senderId).receive();
        if (res == null) {
            long startTime = System.nanoTime();
            while (res == null  && System.nanoTime() < startTime + 1000000*TIME_OUT_MS) {
                res = networks.get(senderId).receive();
            }
            long nowTime = System.nanoTime();
            networkTime += nowTime-startTime;
        }
//        Serializable res = networks.get(senderId).receive();
//        if (res == null) {
//            long startTime = System.nanoTime();
//            while (res == null && System.currentTimeMillis() < startTime + TIME_OUT_MS) {
//                res = networks.get(senderId).receive();
//                try {
//                    Thread.sleep(WAIT_MS);
//                } catch (InterruptedException e) {
//                    throw new RuntimeException(e);
//                }
//            }
//            long nowTime = System.nanoTime();
//            networkTime += nowTime-startTime;
//        }
        return res;
    }

    @Override
    public void sendToAll(Serializable data) throws NetworkException {
        // TODO can be realized with a pivot party in semi honest case
        for (IP2P network: networks.values()) {
            network.send(data);
        }
    }

    @Override
    public int getNoOfParties() {
        return parties;
    }


    @Override
    public <T extends Serializable> Map<Integer, T> receiveFromAllPeers() {
        Map<Integer, T> values = new HashMap<>(getNoOfParties());
        for (int i : peers()) {
            values.put(i, (T) receive(i));
        }
        return values;
    }

    @Override
    public List<Integer> peers() {
        return peers;
    }

    @Override
    public int myId() {
        return myId;
    }

    public void resetCount() {
        networkTime = 0;
        for (DummyP2P network: networks.values()) {
            network.resetCount();
        }
    }

    public long getBytesSent() {
        long bytes = 0L;
        for (DummyP2P network: networks.values()) {
            bytes += network.getBytesSent();
        }
        return bytes;
    }

    public int getTransfers() {
        int transfers = 0;
        for (DummyP2P network: networks.values()) {
            transfers += network.getTransfers();
        }
        return transfers;
    }

    public int getRounds() {
        int rounds = 0;
        for (DummyP2P network: networks.values()) {
            rounds = Math.max(network.getRounds(), rounds);
        }
        return rounds;
    }

    public long getNetworkTime() {
        // convert to ms
        return networkTime/1000000;
    }

}
