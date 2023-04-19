package dk.jot2re.mult;

import dk.jot2re.network.INetwork;

import java.math.BigInteger;
import java.util.Random;

/**
 * Multiplication class for debugging and protocol benchmark.
 * There is no communication as the pivot server holds everything in plain and all other servers just hold 0 values.
 */
public class PlainMult implements IMult<IntegerShare> {
    private final int pivotId;
    private Random rand;
    private INetwork network;
    private IntegerShare defaultResponse = new IntegerShare(BigInteger.ZERO);

    /**
     * Initializes a new multiplication functionality with the designated party as pivot
     * @param pivotId
     */
    public PlainMult(int pivotId) {
        this.pivotId = pivotId;
    }

    @Override
    public void init(INetwork network) {
        this.network = network;
        this.rand = new Random(DummyMult.class.hashCode() + network.myId());
    }

    @Override
    public BigInteger mult(BigInteger shareA, BigInteger shareB, BigInteger modulo, int upperBound) {
        if (shareA == null || shareB == null || modulo == null) {
            throw new NullPointerException("Input for multiplication as to be non-null");
        }
        if (network.myId() == pivotId) {
            return shareA.multiply(shareB).mod(modulo);
        } else {
            return open(defaultResponse, modulo);
        }
    }

    @Override
    public IntegerShare share(BigInteger value, BigInteger modulo) {
        if (network.myId() == pivotId) {
            return new IntegerShare(value, modulo);
        } else {
            return defaultResponse;
        }
    }

    @Override
    public IntegerShare share(int partyId, BigInteger modulo) {
        return defaultResponse;
    }

    @Override
    public BigInteger open(IntegerShare share, BigInteger modulo) {
        return share.getRawShare();
    }

    @Override
    public IntegerShare multShares(IntegerShare left, IntegerShare right, BigInteger modulo) {
        return new IntegerShare(left.getRawShare().multiply(right.getRawShare()).mod(modulo));
    }

    @Override
    public IntegerShare multConst(IntegerShare share, BigInteger known, BigInteger modulo) {
        return new IntegerShare(share.getRawShare().multiply(known), modulo);
    }

    public void setDefaultResponse(IntegerShare response) {
        this.defaultResponse = response;
    }
}
