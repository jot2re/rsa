package dk.jot2re.mult;

import dk.jot2re.network.INetwork;
import dk.jot2re.rsa.our.RSAUtil;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public abstract class AbstractAdditiveMult implements IMult<IntegerShare> {
    protected Random rand;
    protected INetwork network;

    @Override
    public void init(INetwork network) {
        this.network = network;
        this.rand = new SecureRandom();
    }

    @Override
    public synchronized BigInteger mult(BigInteger myA, BigInteger myB, BigInteger modulo, int upperBound) {
        if (myA == null || myB == null || modulo == null) {
            throw new NullPointerException("Input for multiplication as to be non-null");
        }
        List<IntegerShare> aShares = new ArrayList<>(network.getNoOfParties());
        List<IntegerShare> bShares = new ArrayList<>(network.getNoOfParties());
        for (int i = 0; i < network.getNoOfParties(); i++) {
            if (network.myId() == i) {
                aShares.add(share(myA, modulo));
                bShares.add(share(myB, modulo));
            } else {
                aShares.add(share(i, modulo));
                bShares.add(share(i, modulo));
            }
        }
        IntegerShare a = aShares.stream().reduce(new IntegerShare(BigInteger.ZERO),
                (cur, next) -> new IntegerShare(cur.getRawShare().add(next.getRawShare()), modulo));
        IntegerShare b = bShares.stream().reduce(new IntegerShare(BigInteger.ZERO),
                (cur, next) -> new IntegerShare(cur.getRawShare().add(next.getRawShare()), modulo));
        BigInteger plainValue = open(multShares(a, b, modulo), modulo);
        if (network.myId() == 0) {
            return share(plainValue, modulo).getRawShare();
        } else {
            return share(0, modulo).getRawShare();
        }
//        return multShares(new IntegerShare(myA), new IntegerShare(myB), modulo).getRawShare();
    }

    @Override
    public IntegerShare share(BigInteger value, BigInteger modulo) {
        BigInteger randomSum = BigInteger.ZERO;
        for (int i : network.peers()) {
            BigInteger share = RSAUtil.sample(rand, modulo);
            randomSum = randomSum.add(share);
            network.send(i, share);
        }
        // Compute the share of the pivot party
        return new IntegerShare(value.subtract(randomSum), modulo);
    }

    @Override
    public IntegerShare share(int partyId, BigInteger modulo) {
        return new IntegerShare(network.receive(partyId), modulo);
    }

    @Override
    public BigInteger open(IntegerShare share, BigInteger modulo) {
        try {
            network.sendToAll(share.getRawShare());
            Map<Integer, BigInteger> shares = network.receiveFromAllPeers();
            return shares.values().stream().reduce(share.getRawShare(), BigInteger::add).mod(modulo);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

//    @Override
//    public IShare add(IShare left, IShare right) {
//        return new IntegerShare(left.getRawShare().add(right.getRawShare()), left.getModulo());
//    }
//
//    @Override
//    public IShare add(IShare share, BigInteger known) {
//        return null;
//    }

    @Override
    abstract public IntegerShare multShares(IntegerShare left, IntegerShare right, BigInteger modulo);


    @Override
    public IntegerShare multConst(IntegerShare share, BigInteger known, BigInteger modulo) {
        return new IntegerShare(share.getRawShare().multiply(known), modulo);
    }
}
