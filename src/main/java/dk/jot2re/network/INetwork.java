package dk.jot2re.network;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface INetwork {
    void init();
    void send(int recipientId, Serializable data) throws NetworkException;
    Serializable receive(int senderId);
    void sendToAll(Serializable data) throws NetworkException;

    /**
     * @return Peers, excluding the current party.
     */
    List<Integer> peers();
    int myId();

    default <T extends Serializable> Map<Integer, T> receiveFromAllPeers() {
        Map<Integer, T> values = new HashMap<>(peers().size());
        for (int i : peers()) {
            values.put(i, (T) receive(i));
        }
        return values;
    }

    /**
     * Receives data from all parties except the pivot.
     * Pivot is assumed to be party 0
     */
    default <T extends Serializable> Map<Integer, T> receiveFromNonPivotPeers() {
        Map<Integer, T> values = new HashMap<>(peers().size());
        for (int i : peers()) {
            if (i == 0) {
                continue;
            }
            values.put(i, (T) receive(i));
        }
        return values;
    }
}
