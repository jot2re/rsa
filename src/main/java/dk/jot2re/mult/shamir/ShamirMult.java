package dk.jot2re.mult.shamir;

import dk.jot2re.mult.IMult;
import dk.jot2re.mult.ot.util.MaliciousException;
import dk.jot2re.mult.ot.util.Pair;
import dk.jot2re.network.INetwork;
import dk.jot2re.network.NetworkException;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import static dk.jot2re.mult.ot.util.Fiddling.ceil;

public class ShamirMult implements IMult {
    private final ShamirResourcePool resourcePool;
    private final ShamirEngine engine;
    private final int maxCorrupt;
    private INetwork network;

    public ShamirMult(ShamirResourcePool resourcePool) {
        this.resourcePool = resourcePool;
        this.engine = new ShamirEngine(resourcePool.getParties(), resourcePool.getRng());
        this.maxCorrupt = resourcePool.getParties()-ceil(resourcePool.getParties(), 2);
    }

    @Override
    public void init(INetwork network) {
        this.network = network;
    }

    @Override
    public BigInteger mult(BigInteger shareA, BigInteger shareB, BigInteger modulo, int upperBound) {
        if (shareA == null || shareB == null || modulo == null) {
            throw new NullPointerException("Input for multiplication as to be non-null");
        }
        try {
            BigInteger A = sharedInput(shareA, modulo);
            BigInteger B = sharedInput(shareB, modulo);
            return degreeReduction(A.multiply(B).mod(modulo), modulo);
        } catch (Exception e) {
            throw new RuntimeException("Failed to multiply", e);
        }
    }

    public BigInteger combine(int degree, List<BigInteger> shares, BigInteger modulo) {
        return engine.combine(degree, shares, modulo);
    }

    protected BigInteger sharedInput(BigInteger share, BigInteger modulo) {
        BigInteger myShare = shareMyValue(resourcePool.getThreshold(), share, modulo);
        Map<Integer, BigInteger> peerShares = network.receiveFromAllPeers();
        for (int i : peerShares.keySet()) {
            if (i != resourcePool.getMyId()) {
                myShare = myShare.add(peerShares.get(i));
            }
        }
        return myShare.mod(modulo);
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
        BigInteger random = resourcePool.getRng().nextBigInteger(modulo);
        BigInteger myLargeShare = shareMyValue(resourcePool.getThreshold()*2, random, modulo);
        BigInteger mySmallShare = shareMyValue(resourcePool.getThreshold(), random, modulo);
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
