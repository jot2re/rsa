package dk.jot2re.mult;

import dk.jot2re.network.INetwork;

import java.math.BigInteger;
import java.util.Random;

public class DummyMult implements IMult {
    public static final int DEFAULT_SHARE_SIZE = 128;
    private BigInteger modulo;
    private Random rand;
    private int shareSizeBits;
    private INetwork network;
    private int calls = 0;

    public DummyMult() {
        this.modulo = null;
        this.shareSizeBits = DEFAULT_SHARE_SIZE;
    }

    @Override
    public void init(BigInteger modulo, INetwork network) {
        this.modulo = modulo;
        this.shareSizeBits = modulo.bitLength();
        this.network = network;
        this.rand = new Random(DummyMult.class.hashCode() + network.myId());
    }

    @Override
    public BigInteger mult(BigInteger shareA, BigInteger shareB) {
        try {
            calls++;
            if (network.myId() != 0) {
                network.send(0, shareA);
                network.send(0, shareB);
                // All except the pivot party picks their own shares
                BigInteger shareC = new BigInteger(shareSizeBits, rand);
                network.send(0, shareC);
                return shareC;
            }
            BigInteger A = shareA;
            BigInteger B = shareB;
            BigInteger C = BigInteger.ZERO;
            for (int i : network.peers()) {
                A = A.add(network.receive(i));
                B = B.add(network.receive(i));
                C = C.add(network.receive(i));
            }
            BigInteger refC = A.multiply(B);
            // Compute the share of the pivot party
            BigInteger shareC = refC.subtract(C);
            if (modulo != null) {
                return shareC.mod(modulo);
            } else {
                return shareC;
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to multiply", e);
        }
    }

    public int getCalls() {
        return calls;
    }
}
