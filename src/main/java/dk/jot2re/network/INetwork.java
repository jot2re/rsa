package dk.jot2re.network;

import java.io.Serializable;

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

}
