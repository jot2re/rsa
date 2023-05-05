package anonymous.mult;

import anonymous.mult.ot.util.Fiddling;

import java.math.BigInteger;

public class DummyMult extends AbstractAdditiveMult {
    private int multCalls = 0;
    private long bytesSent = 0l;
    private long bytesReceived = 0l;
    public DummyMult() {
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
                BigInteger shareC = new BigInteger(modulo.bitLength(), random);
                network.send(0, shareC);
                bytesSent += Fiddling.ceil(3*modulo.bitLength(), 8);
                return shareC;
            }
            BigInteger A = shareA;
            BigInteger B = shareB;
            BigInteger C = BigInteger.ZERO;
            bytesReceived += (network.getNoOfParties()-1)* Fiddling.ceil(3*modulo.bitLength(), 8);
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

    public long bytesSend() {
        return bytesSent;
    }

    public long bytesReceived() {
        return bytesReceived;
    }

    public int getMultCalls() {
        return multCalls;
    }
}
