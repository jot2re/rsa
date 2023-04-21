package dk.jot2re.mult.shamir;

import dk.jot2re.AbstractProtocol;
import dk.jot2re.mult.IMult;
import dk.jot2re.mult.ot.util.MaliciousException;
import dk.jot2re.mult.ot.util.Pair;
import dk.jot2re.network.INetwork;
import dk.jot2re.network.NetworkException;
import dk.jot2re.rsa.our.RSAUtil;

import java.math.BigInteger;
import java.util.Map;
import java.util.Random;

public class ShamirMult extends AbstractProtocol implements IMult<BigInteger> {
    private final ShamirResourcePool resourcePool;
    private ShamirEngine engine;

    public ShamirMult(ShamirResourcePool resourcePool) {
        this.resourcePool = resourcePool;
    }

    @Override
    public void init(INetwork network, Random random) {
        super.init(network, random);
        this.engine = new ShamirEngine(resourcePool.getParties(), random);
    }

    @Override
    public BigInteger share(BigInteger value, BigInteger modulo) {
        return shareMyValue(resourcePool.getThreshold(), value, modulo);
    }

    @Override
    public BigInteger share(int partyId, BigInteger modulo) {
        return network.receive(partyId);
    }

    @Override
    public BigInteger multShares(BigInteger shareA, BigInteger shareB, BigInteger modulo) {
        if (shareA == null || shareB == null || modulo == null) {
            throw new NullPointerException("Input for multiplication as to be non-null");
        }
        try {
            return degreeReduction(shareA.multiply(shareB).mod(modulo), modulo);
        } catch (Exception e) {
            throw new RuntimeException("Failed to multiply", e);
        }
    }

    @Override
    public BigInteger multConst(BigInteger share, BigInteger known, BigInteger modulo) {
        return share.multiply(known).mod(modulo);
    }

    @Override
    public BigInteger sub(BigInteger left, BigInteger right, BigInteger modulo) {
        return left.subtract(right).mod(modulo);
    }
    @Override
    public BigInteger add(BigInteger left, BigInteger right, BigInteger modulo) {
        return left.add(right).mod(modulo);
    }

    @Override
    public BigInteger addConst(BigInteger share, BigInteger known, BigInteger modulo) {
        return share.add(known).mod(modulo);
    }

    @Override
    public BigInteger open(BigInteger share, BigInteger modulo) {
        try {
            network.sendToAll(share);
            Map<Integer, BigInteger> otherShares = network.receiveFromAllPeers();
            otherShares.put(resourcePool.getMyId(), share);
            return engine.combine(resourcePool.getThreshold(), otherShares, modulo);
        } catch (NetworkException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public BigInteger shareFromAdditive(BigInteger share, BigInteger modulo) {
        BigInteger myShare = shareMyValue(resourcePool.getThreshold(), share, modulo);
        Map<Integer, BigInteger> peerShares = network.receiveFromAllPeers();
        for (int i : peerShares.keySet()) {
            if (i != resourcePool.getMyId()) {
                myShare = myShare.add(peerShares.get(i));
            }
        }
        return myShare.mod(modulo);
    }

    @Override
    public BigInteger combineToAdditive(BigInteger share, BigInteger modulo) {
        if (resourcePool.getMyId() < resourcePool.getThreshold()+1) {
            return share.multiply(engine.lagrangeConst(resourcePool.getMyId()+1, resourcePool.getThreshold(), modulo)).mod(modulo);
        } else {
            return BigInteger.ZERO;
        }
    }

    protected BigInteger shareMyValue(int degree, BigInteger value, BigInteger modulo) {
        // todo remove need on map
        Map<Integer, BigInteger> sharesOfValue = engine.randomPoly(degree, value, modulo);
        for (int i = 0; i < resourcePool.getParties(); i++) {
            if (i != resourcePool.getMyId()) {
                network.send(i, sharesOfValue.get(i));
            }
        }
        return sharesOfValue.get(resourcePool.getMyId());
    }

    // TODO does not work, but I have no idea why
    protected BigInteger bgwDegreeReduction(BigInteger value, BigInteger modulo) {
//        // TODO so the protocol calls for degree 2*t, but this does not work
//        BigInteger myShare = shareMyValue(resourcePool.getThreshold()*2, value, modulo);
//        Map<Integer, BigInteger> othersShares = network.receiveFromAllPeers();
//        othersShares.put(resourcePool.getMyId(), myShare);
//        BigInteger res = BigInteger.ZERO;
//        for (int i = 0; i < othersShares.size(); i++) {
//            res = res.add(othersShares.get(i).multiply(engine.degreeRedConst(resourcePool.getMyId(), i, modulo)));
//        }
//        return res.mod(modulo);
        // TODO can be optimized with sharing a seed for generating shares s.t. only last party needs to receive a point on the poly
        BigInteger myShare = shareMyValue(resourcePool.getThreshold()*2, BigInteger.ZERO, modulo);
        Map<Integer, BigInteger> othersShares = network.receiveFromAllPeers();
        othersShares.put(resourcePool.getMyId(), myShare);
        BigInteger contri = othersShares.values().stream().reduce(value, BigInteger::add);
        BigInteger finalShare = shareMyValue(resourcePool.getThreshold(), contri, modulo);
        Map<Integer, BigInteger> peerShares = network.receiveFromAllPeers();
        peerShares.put(resourcePool.getMyId(), finalShare);
        BigInteger res = BigInteger.ZERO;
        for (int i : peerShares.keySet()) {
                res = res.add(peerShares.get(i).multiply(engine.degreeRedConst(resourcePool.getMyId(), i, modulo)));
        }
        return res.mod(modulo);
    }

    protected BigInteger degreeReduction(BigInteger value, BigInteger modulo) throws NetworkException {
        Pair<BigInteger, BigInteger> pair = randomPair(modulo);
        BigInteger myPaddedShare = value.add(pair.getFirst()).mod(modulo);
        network.sendToAll(myPaddedShare);
        Map<Integer, BigInteger> paddedShares = network.receiveFromAllPeers();
        paddedShares.put(resourcePool.getMyId(), myPaddedShare);
        BigInteger paddedProduct = engine.combine(resourcePool.getThreshold()*2, paddedShares, modulo);
        return paddedProduct.subtract(pair.getSecond());
    }

    protected Pair<BigInteger, BigInteger> randomPair(BigInteger modulo) {
        BigInteger r = RSAUtil.sample(random, modulo);
        BigInteger myLargeShare = shareMyValue(resourcePool.getThreshold()*2, r, modulo);
        BigInteger mySmallShare = shareMyValue(resourcePool.getThreshold(), r, modulo);
        Map<Integer, BigInteger> largePeerShares = network.receiveFromAllPeers();
        Map<Integer, BigInteger> smallPeerShares = network.receiveFromAllPeers();
        if (largePeerShares.size() != smallPeerShares.size() || smallPeerShares.size() != resourcePool.getParties()-1) {
            throw new MaliciousException("Incorrect amount of shares");
        }
        for (int i = 0; i < resourcePool.getParties(); i++) {
            if (i != resourcePool.getMyId()) {
                myLargeShare = myLargeShare.add(largePeerShares.get(i));
                mySmallShare = mySmallShare.add(smallPeerShares.get(i));
            }
        }
        return new Pair<>(myLargeShare.mod(modulo), mySmallShare.mod(modulo));
    }
}
