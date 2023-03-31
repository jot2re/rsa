package dk.jot2re.mult.gilboa;

import dk.jot2re.mult.gilboa.ot.otextension.OtExtensionResourcePool;
import dk.jot2re.mult.gilboa.ot.otextension.RotReceiver;
import dk.jot2re.mult.gilboa.util.ByteArrayHelper;
import dk.jot2re.mult.gilboa.util.StrictBitVector;
import dk.jot2re.network.INetwork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.List;

import static dk.jot2re.mult.gilboa.GilboaOTFactory.expand;

public class GilboaOTReceiver {
    private static final Logger logger = LoggerFactory.getLogger(GilboaOTReceiver.class);
    private final RotReceiver receiver;
    private final OtExtensionResourcePool resources;
    private final INetwork network;
    private final int batchSize;

    // The random messages received from the batched random 1-out-of-2 OTs
    private List<StrictBitVector> randomMessages;
    // The random choices from the batched random 1-out-of-2 OTs
    private StrictBitVector choices;
    // Index of the current random OT to use
    private int offset = -1;

    public GilboaOTReceiver(RotReceiver rotReceiver,
                             OtExtensionResourcePool resources, INetwork network, int batchSize) {
        this.receiver = rotReceiver;
        this.resources = resources;
        this.network = network;
        this.batchSize = batchSize;
    }

    public void makeBatch() {
        logger.info("Making a new batch of OTs...");
        choices = new StrictBitVector(batchSize, resources.getRandomGenerator());
        randomMessages = receiver.extend(choices);
        offset = 0;
    }

    public BigInteger receive(boolean choiceBit, int amountBytes) {
        // Check if there is still an unused random OT stored, if not, execute a
        // random OT extension
        if (offset < 0 || offset >= batchSize) {
            makeBatch();
        }
        // Notify the sender if it should switch the 0 and 1 messages around (s.t.
        // the random choice bit in the preprocessed random OTs matches the true
        // choice bit
        boolean switchBit = sendSwitchBit(choiceBit);
        network.send(resources.getOtherId(), switchBit);
        // Receive the serialized adjusted messages
        byte[] adjustment = network.receive(resources.getOtherId());
        byte[] message = randomMessages.get(offset).toByteArray();
        byte[] messageExpanded = expand(message, amountBytes);
        if (choiceBit == true ) {
            ByteArrayHelper.xor(messageExpanded, adjustment);
        }
        BigInteger oneVal = new BigInteger(1, messageExpanded);
        offset++;
        return oneVal;
    }

    /**
     * Compute and send a bit indicating whether the sender should switch the zero
     * and one message around.
     *
     * @param choiceBit
     *          The actual choice bit of the receiver
     */
    private boolean sendSwitchBit(boolean choiceBit) {
        // Since we can only send a byte array we use 0x00 to indicate a 0-choice
        // and 0x01 to indicate a 1-choice
        byte[] switchBit = new byte[] { 0x00 };
        // Set the choice to 0x01 if the sender should switch the 0 and 1 messages
        if (choiceBit ^ choices.getBit(offset, false) == true) {
            switchBit[0] = 0x01;
        }
        return switchBit[0] == 0x01 ? true : false;
    }
}
