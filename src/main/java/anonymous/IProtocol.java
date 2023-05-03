package anonymous;

import anonymous.network.INetwork;

import java.util.Random;

public interface IProtocol {
    public void init(INetwork network, Random random);
}
