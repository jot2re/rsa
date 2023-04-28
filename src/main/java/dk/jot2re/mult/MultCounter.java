package dk.jot2re.mult;

import dk.jot2re.network.DummyNetwork;
import dk.jot2re.network.INetwork;

import java.io.Serializable;
import java.math.BigInteger;
import java.time.Instant;
import java.util.Random;

public class MultCounter<T extends Serializable> implements IMult<T> {
    private long timeSpent = 0;
    private long bytesSent;
    private long bytesReceived;
    private int mults = 0;
    private int constMults = 0;
    private int fromAdd = 0;
    private int toAdd = 0;
    private int opens = 0;
    private int sharings = 0;
    private long startTime;
    private long stopTime;
    private long startBytesSend;
    private long stopBytesSend;
    private long startBytesReceived;
    private long stopBytesReceived;
    private DummyNetwork network;
    private final IMult<T> internalMult;
    private boolean initialized = false;

    public MultCounter(IMult<T> internalMult) {
        this.internalMult = internalMult;
    }

    public IMult getDecorated() {
        return internalMult;
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
        startBytesSend = network.getBytesSent();
        startBytesReceived = network.getBytesReceived();
    }

    private void stopBytes() {
        stopBytesSend = network.getBytesSent();
        stopBytesReceived = network.getBytesReceived();
        bytesSent += stopBytesSend - startBytesSend;
        bytesReceived += stopBytesReceived - startBytesReceived;
    }

    public long getBytesSent() {
        return bytesSent;
    }

    public long getBytesReceived() {
        return bytesReceived;
    }

    public long getTimeSpent() {
        return timeSpent;
    }

    @Override
    public void init(INetwork network, Random random) {
        if (!initialized) {
            this.network = (DummyNetwork) network;
            internalMult.init(network, random);
            initialized = true;
        }
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
                "Total bytes rec*:  " + bytesReceived + "\n" +
                "Total time:        " + (timeSpent) + " nano sec";
    }
}
