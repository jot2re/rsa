package dk.jot2re.mult.ot.gilboa;

import dk.jot2re.mult.ot.ot.otextension.OtExtensionResourcePool;
import dk.jot2re.mult.ot.ot.otextension.RotSender;
import dk.jot2re.mult.ot.util.ByteArrayHelper;
import dk.jot2re.mult.ot.util.MaliciousException;
import dk.jot2re.mult.ot.util.Pair;
import dk.jot2re.mult.ot.util.StrictBitVector;
import dk.jot2re.network.INetwork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class GilboaOTSender {
    private static final Logger logger = LoggerFactory.getLogger(GilboaOTSender.class);
    private final RotSender sender;
    private final OtExtensionResourcePool resources;
    private final INetwork network;
    private final int batchSize;

    // The random messages generated in the underlying random OT functionality
    private Pair<List<StrictBitVector>, List<StrictBitVector>> randomMessages;
    // Index of the current random OT to use
    private int offset = -1;

    public GilboaOTSender(RotSender rotSender, OtExtensionResourcePool resources, INetwork network,
                           int batchSize) {
        this.sender = rotSender;
        this.resources = resources;
        this.network = network;
        this.batchSize = batchSize;
    }

    public void makeBatch() {
        logger.info("Making a new batch of OTs...");
        randomMessages = sender.extend(batchSize);
        offset = 0;
    }

    public List<BigInteger> send(BigInteger value, BigInteger modulo) {
        return send(value, modulo, modulo.bitLength());
    }

    public List<BigInteger> send(BigInteger value, BigInteger modulo, int upperBound) {
        // Check if there is still an unused random OT stored, if not, execute a
        // random OT extension
        if (offset < 0 || offset + upperBound >= batchSize) {
            makeBatch();
        }
        StrictBitVector switchBits = network.receive(resources.getOtherId());
        if (switchBits.getSize() != upperBound) {
            throw new MaliciousException("Unexpected size of switchbit vector");
        }
        List<BigInteger> shares = new ArrayList<>(switchBits.getSize());
        ArrayList<byte[]> corrections = new ArrayList<>(switchBits.getSize());
        for (int i = 0; i < switchBits.getSize(); i++) {
            Payload payload = doActualSend(switchBits.getBit(i, false), value, modulo);
            shares.add(payload.getSenderShare());
            corrections.add(payload.getReceiverCorrections());
            offset++;
        }
        network.send(resources.getOtherId(), corrections);
        return shares;
    }

    private Payload doActualSend(boolean switchBit, BigInteger value, BigInteger modulo) {
        // Find the correct preprocessed random OT messages
        StrictBitVector randomZero = randomMessages.getFirst().get(offset);
        StrictBitVector randomOne = randomMessages.getSecond().get(offset);
        int amountBytes = modulo.bitLength() / 8;

        // todo unsafe expansion
        byte[] randZeroBytes = GilboaOTFactory.expand(randomZero.toByteArray(), amountBytes);
        byte[] randOneBytes = GilboaOTFactory.expand(randomOne.toByteArray(), amountBytes);
        Payload payload;
        if (switchBit == false) {
            BigInteger randZero = new BigInteger(1, randZeroBytes).mod(modulo);
            BigInteger randZeroPlusVal = randZero.add(value).mod(modulo);
            byte[] randZeroPlusValBytes = moveToArray(randZeroPlusVal, amountBytes);
            ByteArrayHelper.xor(randOneBytes, randZeroPlusValBytes);
            payload = new Payload(randZero, randOneBytes);
        } else {
            BigInteger randOne = new BigInteger(1, randOneBytes).mod(modulo);
            BigInteger randOnePlusVal = randOne.add(value).mod(modulo);
            byte[] randOnePlusValBytes = moveToArray(randOnePlusVal, amountBytes);
            ByteArrayHelper.xor(randZeroBytes, randOnePlusValBytes);
            payload = new Payload(randOne, randZeroBytes);
        }
        return payload;
    }

    /**
     * Takes a UNSIGNED BigInteger and moves it into a byte array of certain size.
     * Note that no safety checks are performed of whether there is space enough
     */
    protected static byte[] moveToArray(BigInteger value, int bytes) {
        byte[] valueBytes = value.toByteArray();
        byte[] adjustedBytes = new byte[bytes];
        for (int j = 0; j < bytes; j++) {
            if (valueBytes.length > j) {
                adjustedBytes[adjustedBytes.length-j-1] = valueBytes[valueBytes.length-j-1];
            }
        }
        return adjustedBytes;
    }

    public static class Payload {
        private final BigInteger senderShare;
        private final byte[] receiverCorrections;

        public Payload(BigInteger senderShare, byte[] receiverCorrections) {
            this.senderShare = senderShare;
            this.receiverCorrections = receiverCorrections;
        }

        public BigInteger getSenderShare() {
            return senderShare;
        }

        public byte[] getReceiverCorrections() {
            return receiverCorrections;
        }
    }
}
