package dk.jot2re.mult;

import dk.jot2re.network.DummyNetwork;
import dk.jot2re.network.INetwork;

import java.io.Serializable;
import java.math.BigInteger;
import java.time.Instant;

public class MultCounter<T extends Serializable> implements IMult<T> {
    private long timeSpent = 0;
    private long bytesSent;
    private int mults = 0;
    private int constMults = 0;
    private int fromAdd = 0;
    private int toAdd = 0;
    private int opens = 0;
    private int sharings = 0;
    private long startTime;
    private long stopTime;
    private long startBytes;
    private long stopBytes;
    private DummyNetwork network;
    private final IMult<T> internalMult;

    public MultCounter(IMult<T> internalMult) {
        this.internalMult = internalMult;
    }

    private void start() {
        startBytes();
        startTime();
    }

    private void stop() {
        stopTime();
        stopBytes();
    }

    private void startTime() {
        startTime = Instant.now().getNano();
    }

    private void stopTime() {
        stopTime = Instant.now().getNano();
        timeSpent += stopTime - startTime;
    }

    private void startBytes() {
        startBytes = network.getBytesSent();
    }

    private void stopBytes() {
        stopBytes = network.getBytesSent();
        bytesSent += stopBytes-startBytes;
    }

    public long getBytesSent() {
        return bytesSent;
    }

    public long getBytesReceived() {
        return bytesSent*(network.getNoOfParties()-1);
    }

    public long getTimeSpent() {
        return timeSpent;
    }

    @Override
    public void init(INetwork network) {
        this.network = (DummyNetwork) network;
        internalMult.init(network);
    }

    @Override
    public T share(BigInteger value, BigInteger modulo) {
        sharings++;
        start();
        T res = internalMult.share(value, modulo);
        stop();
        return res;
    }

    @Override
    public T share(int partyId, BigInteger modulo) {
        start();
        T res = internalMult.share(partyId, modulo);
        stop();
        return res;
    }

    @Override
    public T shareFromAdditive(BigInteger value, BigInteger modulo) {
        fromAdd++;
        start();
        T res =  internalMult.shareFromAdditive(value, modulo);
        stop();
        return res;
    }

    @Override
    public BigInteger combineToAdditive(T share, BigInteger modulo) {
        toAdd++;
        start();
        BigInteger res = internalMult.combineToAdditive(share, modulo);
        stop();
        return res;
    }

    @Override
    public T multShares(T left, T right, BigInteger modulo) {
        mults++;
        start();
        T res = internalMult.multShares(left, right, modulo);
        stop();
        return res;
    }

    @Override
    public T multConst(T share, BigInteger known, BigInteger modulo) {
        constMults++;
        start();
        T res = internalMult.multConst(share, known, modulo);
        stop();
        return res;
    }

    @Override
    public T add(T left, T right, BigInteger modulo) {
        start();
        T res = internalMult.add(left, right, modulo);
        stop();
        return res;
    }

    @Override
    public T sub(T left, T right, BigInteger modulo) {
        start();
        T res = internalMult.sub(left, right, modulo);
        stop();
        return res;
    }

    @Override
    public T addConst(T share, BigInteger known, BigInteger modulo) {
        start();
        T res = internalMult.addConst(share, known, modulo);
        stop();
        return res;
    }

    @Override
    public BigInteger open(T share, BigInteger modulo) {
        opens++;
        start();
        BigInteger res = internalMult.open(share, modulo);
        stop();
        return res;
    }

    @Override
    public String toString() {
        return  "Multiplications:   " + mults + "\n" +
                "Constant Mults:    " + constMults + "\n" +
                "From additive:     " + fromAdd + "\n" +
                "To additive:       " + toAdd + "\n" +
                "Sharings:          " + sharings + "\n" +
                "Opens:             " + opens + "\n" +
                "Total bytes sent:  " + bytesSent + "\n" +
                "Total bytes rec*:  " + ((network.getNoOfParties()-1)*bytesSent) + "\n" +
                "Total time:        " + timeSpent/1000 + " micro sec";
    }
}
