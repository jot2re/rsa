package dk.jot2re.mult.gilboa;

import dk.jot2re.mult.gilboa.ot.otextension.OtExtensionResourcePool;
import dk.jot2re.mult.gilboa.ot.otextension.RotSender;
import dk.jot2re.mult.gilboa.util.ByteArrayHelper;
import dk.jot2re.mult.gilboa.util.Pair;
import dk.jot2re.mult.gilboa.util.StrictBitVector;
import dk.jot2re.network.INetwork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.List;

import static dk.jot2re.mult.gilboa.GilboaOTFactory.expand;

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

    public BigInteger send(BigInteger value, BigInteger modulo) {
        // Check if there is still an unused random OT stored, if not, execute a
        // random OT extension
        if (offset < 0 || offset >= batchSize) {
            makeBatch();
        }
        BigInteger res = doActualSend(value, modulo);
        offset++;
        return res;
    }

    private BigInteger doActualSend(BigInteger value, BigInteger modulo) {
        // Find the correct preprocessed random OT messages
        StrictBitVector randomZero = randomMessages.getFirst().get(offset);
        StrictBitVector randomOne = randomMessages.getSecond().get(offset);
        int amountBytes = modulo.bitLength()/8;
        BigInteger res;
        // todo unsafe expansion
        byte[] randZeroBytes = expand(randomZero.toByteArray(), amountBytes);
        byte[] randOneBytes = expand(randomOne.toByteArray(), amountBytes);
        // Receive a bit from the receiver indicating whether the zero and one
        // messages should be switched around
        boolean switchBit = network.receive(resources.getOtherId());
        // If false (indicated by byte 0x00), then don't switch around
        if (switchBit == false) {
            BigInteger randZero = new BigInteger(1, randZeroBytes).mod(modulo);
            BigInteger randZeroPlusVal = randZero.add(value).mod(modulo);
            byte[] randZeroPlusValBytes = moveToArray(randZeroPlusVal, amountBytes);
            ByteArrayHelper.xor(randOneBytes, randZeroPlusValBytes);
            res = randZero;
            network.send(resources.getOtherId(),randOneBytes);
        } else {
            BigInteger randOne = new BigInteger(1, randOneBytes).mod(modulo);
            BigInteger randOnePlusVal = randOne.add(value).mod(modulo);
            byte[] randOnePlusValBytes = moveToArray(randOnePlusVal, amountBytes);
            ByteArrayHelper.xor(randZeroBytes, randOnePlusValBytes);
            res = randOne;
            network.send(resources.getOtherId(),randZeroBytes);
        }
        return res;
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
}
