package dk.jot2re;

import dk.jot2re.network.INetwork;

import java.util.Random;

public interface IProtocol {
    public void init(INetwork network, Random random);
}
