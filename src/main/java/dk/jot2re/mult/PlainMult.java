package dk.jot2re.mult;

import dk.jot2re.network.INetwork;

import java.math.BigInteger;
import java.util.Random;

/**
 * Multiplication class for debugging and protocol benchmark.
 * There is no communication as the pivot server holds everything in plain and all other servers just hold 0 values.
 */
public class PlainMult extends AbstractAdditiveMult{
    private final int pivotId;
    private Random rand;
    private INetwork network;
    private int multCalls = 0;
    private BigInteger defaultResponse = BigInteger.ZERO;

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
    public BigInteger multShares(BigInteger left, BigInteger right, BigInteger modulo) {
        multCalls++;
        if (network.myId() == pivotId) {
            return left.multiply(right).mod(modulo);
        } else {
            return defaultResponse;
        }
    }

    public int getMultCalls() {
        return multCalls;
    }

    public void setDefaultResponse(BigInteger response) {
        this.defaultResponse = response;
    }
}
