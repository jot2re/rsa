package dk.jot2re.rsa;

import dk.jot2re.network.INetwork;

import java.security.SecureRandom;

public class Parameters {
    private final int primeBits;
    private final int statBits;
    private final INetwork network;
    private final SecureRandom rand;

    public Parameters(int primeBits, int statBits, INetwork network, SecureRandom rand)  {
        this.statBits = statBits;
        this.primeBits = primeBits;
        this.network = network;
        this.rand = rand;
    }

    public int getPrimeBits() {
        return primeBits;
    }

    public int getStatBits() {
        return statBits;
    }

    public INetwork getNetwork() {
        return network;
    }

    public SecureRandom getRandom() {
        return rand;
    }

    public int getAmountOfPeers() {
        return network.peers().size();
    }

    public int getMyId() {
        return network.myId();
    }
}
