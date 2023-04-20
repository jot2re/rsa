package dk.jot2re.mult.replicated;

import dk.jot2re.mult.IMult;
import dk.jot2re.network.INetwork;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ReplicatedMult implements IMult<ArrayList<BigInteger>> {
    private final ReplictedMultResourcePool resourcePool;
    private final int maxCorrupt;
    private INetwork network;

    public ReplicatedMult(ReplictedMultResourcePool resourcePool) {
        if (resourcePool.getParties() != 3) {
            throw new IllegalArgumentException("Currently only supports 3 parties");
        }
        this.resourcePool = resourcePool;
        this.maxCorrupt = (resourcePool.getParties()-1)/2;
    }

    @Override
    public void init(INetwork network) {
        this.network = network;
    }


    @Override
    public ArrayList<BigInteger> multShares(ArrayList<BigInteger> left, ArrayList<BigInteger> right, BigInteger modulo) {
        BigInteger additiveShare = multiplyShared(left, right, modulo);
        return shareFromAdditive(additiveShare, modulo);
    }

    protected BigInteger multiplyShared(List<BigInteger> A, List<BigInteger> B, BigInteger modulo) {
        BigInteger product = BigInteger.ZERO;
        for (int i = 0; i < A.size(); i++) {
            for (int j = 0; j < B.size(); j++) {
                if (i == 0 || (i > j)) {
                    product = product.add(A.get(i).multiply(B.get(j))).mod(modulo);
                }
            }
        }
        return product;
    }

    @Override
    public ArrayList<BigInteger> shareFromAdditive(BigInteger share, BigInteger modulo) {
        ArrayList<BigInteger> myShare = share(share, modulo);
        Map<Integer, ArrayList<BigInteger>> peerShares = network.receiveFromAllPeers();
        for (int i : peerShares.keySet()) {
            if (i != resourcePool.getMyId()) {
                for (int j = 0; j < resourcePool.getParties()-maxCorrupt; j++) {
                    myShare.set(j, myShare.get(j).add(peerShares.get(i).get(j)).mod(modulo)); // todo optimize, no need to do it after each share
                }
            }
        }
        return myShare;
    }

    @Override
    public BigInteger combineToAdditive(ArrayList<BigInteger> share, BigInteger modulo) {
        return share.get(0);
    }

    @Override
    public ArrayList<BigInteger> share(BigInteger value, BigInteger modulo) {
        ArrayList<BigInteger> sharesOfValue = singlePartyShare(value, modulo);
        for (int i = 0; i < resourcePool.getParties(); i++) {
            if (i != resourcePool.getMyId()) {
                ArrayList<BigInteger> toSend = getPartyShares(i, sharesOfValue);
                network.send(i, toSend);
            }
        }
        return getPartyShares(resourcePool.getMyId(), sharesOfValue);
    }

    @Override
    public ArrayList<BigInteger> share(int partyId, BigInteger modulo) {
        return network.receive(partyId);
    }

    protected ArrayList<BigInteger> getPartyShares(int receiverId, List<BigInteger> shares) {
        // TODO is this the general formula
        ArrayList<BigInteger> toSend = new ArrayList<>(resourcePool.getParties()-1);
        for (int i = 0; i < resourcePool.getParties()-maxCorrupt; i++) {
            int shareIdx = (receiverId+i) % (resourcePool.getParties());
            toSend.add(shares.get(shareIdx));
        }
        return toSend;
    }

    protected ArrayList<BigInteger> singlePartyShare(BigInteger share, BigInteger modulo) {
        ArrayList<BigInteger> shares = new ArrayList<>(resourcePool.getParties());
        BigInteger partialSum = BigInteger.ZERO;
        for (int i = 0; i < resourcePool.getParties()-1; i++) {
            BigInteger curShare = resourcePool.getRng().nextBigInteger(modulo);
            partialSum = partialSum.add(curShare);
            shares.add(curShare);
        }
        shares.add(share.subtract(partialSum).mod(modulo));
        return shares;
    }
}
