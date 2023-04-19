package dk.jot2re.mult;

import dk.jot2re.network.INetwork;

import java.math.BigInteger;
import java.util.Random;

public class DummyMult extends AbstractAdditiveMult {
    private int multCalls = 0;

    public DummyMult() {
    }

    @Override
    public void init(INetwork network) {
        super.network = network;
        super.rand = new Random(DummyMult.class.hashCode() + network.myId());
    }

    @Override
    public synchronized BigInteger mult(BigInteger myA, BigInteger myB, BigInteger modulo, int upperBound) {
        if (myA == null || myB == null || modulo == null) {
            throw new NullPointerException("Input for multiplication as to be non-null");
        }
        multCalls++;
        return multShares(new IntegerShare(myA), new IntegerShare(myB), modulo).getRawShare();
    }

    @Override
    public IntegerShare multShares(IntegerShare left, IntegerShare right, BigInteger modulo) {
        BigInteger openedLeft = open(left, modulo);
        BigInteger openedRight = open(right, modulo);
        BigInteger product = openedLeft.multiply(openedRight).mod(modulo);
        if (network.myId() == 0) {
            return share(product, modulo);
        }
        return share(0, modulo);
    }

    public int getMultCalls() {
        return multCalls;
    }
}
