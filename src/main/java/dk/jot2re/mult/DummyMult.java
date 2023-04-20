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
    public BigInteger multShares(BigInteger shareA, BigInteger shareB, BigInteger modulo) {
        if (shareA == null || shareB == null || modulo == null) {
            throw new NullPointerException("Input for multiplication as to be non-null");
        }
        try {
            multCalls++;
            if (network.myId() != 0) {
                network.send(0, shareA);
                network.send(0, shareB);
                // All except the pivot party picks their own shares
                BigInteger shareC = new BigInteger(modulo.bitLength(), rand);
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

    public int getMultCalls() {
        return multCalls;
    }
}
