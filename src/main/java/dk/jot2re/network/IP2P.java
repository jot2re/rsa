package dk.jot2re.network;

import java.io.Serializable;

public interface IP2P {
    void init();
    void send(Serializable data) throws NetworkException;
    Serializable receive();
    int myId();
    int peerId();
}
