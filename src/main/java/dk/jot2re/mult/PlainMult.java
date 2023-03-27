package dk.jot2re.mult;

import dk.jot2re.network.INetwork;
import dk.jot2re.network.NetworkException;
import dk.jot2re.rsa.Parameters;
import dk.jot2re.rsa.bf.BFParameters;

import java.math.BigInteger;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Multiplication class for debugging and protocol benchmark.
 * There is no communication as the pivot server holds everything in plain and all other servers just hold 0 values.
 */
public class PlainMult implements IMult {
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
    public BigInteger mult(BigInteger shareA, BigInteger shareB, BigInteger modulo) {
        if (shareA == null || shareB == null || modulo == null) {
            throw new NullPointerException("Input for multiplication as to be non-null");
        }
        multCalls++;
        if (network.myId() == pivotId) {
            return shareA.multiply(shareB).mod(modulo);
        } else {
            return defaultResponse;
        }
    }

    @Override
    public List<BigInteger> sample(Parameters params, BigInteger modulo, int amount) {
        return IntStream.range(0, amount).mapToObj(i -> sample(params, modulo)).collect(Collectors.toList());
    }

    @Override
    public BigInteger sample(Parameters params, BigInteger modulo) {
        BigInteger r = new BigInteger(modulo.bitLength()+params.getStatBits(), params.getRandom());
        return r.mod(modulo);
    }

    @Override
    public BigInteger open(Parameters params, BigInteger share, BigInteger modulo) throws NetworkException {
        if (network.myId() == pivotId) {
            return share;
        } else {
            return defaultResponse;
        }
    }

    @Override
    public BigInteger addConst(Parameters params, BigInteger share, BigInteger constant, BigInteger modulo) throws NetworkException {
        if (network.myId() == pivotId) {
            return share.add(constant).mod(modulo);
        } else {
            return share;
        }
    }

    @Override
    public BigInteger subConst(Parameters params, BigInteger share, BigInteger constant, BigInteger modulo) throws NetworkException {
        if (network.myId() == pivotId) {
            return share.subtract(constant).mod(modulo);
        } else {
            return share;
        }
    }

    @Override
    public BigInteger multList(BFParameters params, BigInteger[] shares, BigInteger modulo) {
        if (shares.length < 2) {
            throw new IllegalArgumentException("Empty or singleton list");
        }
        BigInteger temp = params.getMult().mult(shares[0], shares[1], modulo);
        for (int i = 2; i < shares.length; i++) {
            temp = params.getMult().mult(temp, shares[i], modulo);
        }
        return temp;
    }

    public int getMultCalls() {
        return multCalls;
    }

    public void setDefaultResponse(BigInteger response) {
        this.defaultResponse = response;
    }
}
