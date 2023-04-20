package dk.jot2re.mult;

import dk.jot2re.network.INetwork;
import dk.jot2re.network.NetworkException;
import dk.jot2re.rsa.our.RSAUtil;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Map;
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
        return value;
    }

    @Override
    public BigInteger combineToAdditive(BigInteger share, BigInteger modulo) {
        return share;
    }

    @Override
    public abstract BigInteger multShares(BigInteger left, BigInteger right, BigInteger modulo);

    @Override
    public BigInteger multConst(BigInteger share, BigInteger known, BigInteger modulo) {
        return share.multiply(known).mod(modulo);
    }

    @Override
    public BigInteger sub(BigInteger left, BigInteger right, BigInteger modulo) {
        return left.subtract(right);
    }

    @Override
    public BigInteger add(BigInteger left, BigInteger right, BigInteger modulo) {
        return left.add(right);
    }

    @Override
    public BigInteger addConst(BigInteger share, BigInteger known, BigInteger modulo) {
        if (network.myId() == 0) {
            return share.add(known);
        } else {
            return share;
        }
    }

    @Override
    public BigInteger open(BigInteger share, BigInteger modulo) {
        try {
            network.sendToAll(share);
            Map<Integer, BigInteger> otherShares = network.receiveFromAllPeers();
            return otherShares.values().stream().reduce(share, (a, b) -> a.add(b)).mod(modulo);
        } catch (NetworkException e) {
            throw new RuntimeException(e);
        }
    }
}
