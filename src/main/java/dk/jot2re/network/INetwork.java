package dk.jot2re.network;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

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

    default <T extends Serializable> List<T> receiveList() {
        List<T> values = new ArrayList<>(peers().size());
        for (int i : peers()) {
            values.add((T) receive(i));
        }
        return values;
    }
}
