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
     * @return Amount of peers, excluding the current party. Thus a positive number of at least 1.
     */
    int peers();
    int myId();

    default <T extends Serializable> List<T> receiveList() {
        List<T> values = new ArrayList<>(peers());
        for (int i = 0; i < peers(); i++)
            if (i != myId()) {
                values.add((T) receive(i));
            }
        return values;
    }
}
