package dk.jot2re.mult;

import dk.jot2re.network.INetwork;
import dk.jot2re.rsa.our.RSAUtil;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Random;

public abstract class AbstractAdditiveMult implements IMult<BigInteger> {
    protected Random rand;
    protected INetwork network;

    @Override
    public void init(INetwork network) {
        this.network = network;
        this.rand = new SecureRandom();
    }

    @Override
    public BigInteger share(BigInteger value, BigInteger modulo) {
        BigInteger randomSum = BigInteger.ZERO;
        for (int i : network.peers()) {
            BigInteger share = RSAUtil.sample(rand, modulo);
            randomSum = randomSum.add(share);
            network.send(i, share);
        }
        // Compute the share of the pivot party
        return value.subtract(randomSum).mod(modulo);
    }

    @Override
    public BigInteger share(int partyId, BigInteger modulo) {
        return network.receive(partyId);
    }

    @Override
    public BigInteger shareFromAdditive(BigInteger value, BigInteger modulo) {
        return value.mod(modulo);
    }

    @Override
    public BigInteger combineToAdditive(BigInteger share, BigInteger modulo) {
        return share.mod(modulo);
    }

    @Override
    public abstract BigInteger multShares(BigInteger left, BigInteger right, BigInteger modulo);
}
