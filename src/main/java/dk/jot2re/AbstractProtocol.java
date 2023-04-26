package dk.jot2re;

import dk.jot2re.network.INetwork;

import java.util.Random;

public abstract class AbstractProtocol implements IProtocol {
    private boolean initialized = false;
    protected INetwork network;
    protected Random random;
    public void init(INetwork network, Random random) {
        if (initialized == false) {
            this.network = network;
            this.random = random;
            initialized = true;
        }
    }

}
