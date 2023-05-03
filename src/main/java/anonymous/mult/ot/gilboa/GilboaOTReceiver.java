package anonymous.mult.ot.gilboa;

import anonymous.mult.ot.ot.otextension.OtExtensionResourcePool;
import anonymous.mult.ot.ot.otextension.RotReceiver;
import anonymous.mult.ot.util.ByteArrayHelper;
import anonymous.mult.ot.util.StrictBitVector;
import anonymous.network.INetwork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class GilboaOTReceiver {
    private static final Logger logger = LoggerFactory.getLogger(GilboaOTReceiver.class);
    private final RotReceiver receiver;
    private final OtExtensionResourcePool resources;
    private final INetwork network;
    private final int batchSize;

    // The random messages received from the batched random 1-out-of-2 OTs
    private List<StrictBitVector> randomMessages;
    // The random choices from the batched random 1-out-of-2 OTs
    private StrictBitVector randomChoices;
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
        randomChoices = new StrictBitVector(batchSize, resources.getRandomGenerator());
        randomMessages = receiver.extend(randomChoices);
        offset = 0;
    }

    public List<BigInteger> receive(StrictBitVector actualChoices, int amountBytes) {
        // Check if there is still an unused random OT stored, if not, execute a
        // random OT extension
        if (offset < 0 || offset+actualChoices.getSize() >= batchSize) {
            makeBatch();
        }
        List<BigInteger> shares = new ArrayList<>(actualChoices.getSize());
        StrictBitVector switchBits = computeSwitchBits(actualChoices);
        network.send(resources.getOtherId(), switchBits);
        ArrayList<byte[]> adjustments = network.receive(resources.getOtherId());
        for (int i = 0; i < actualChoices.getSize(); i++) {
            byte[] message = randomMessages.get(offset).toByteArray();
            byte[] messageExpanded = GilboaOTFactory.expand(message, amountBytes);
            if (actualChoices.getBit(i, false) == true) {
                ByteArrayHelper.xor(messageExpanded, adjustments.get(i));
            }
            shares.add(new BigInteger(1, messageExpanded));
            offset++;
        }
        return shares;
    }

    public StrictBitVector computeSwitchBits(StrictBitVector actualChoices) {
        StrictBitVector switchBits = new StrictBitVector(actualChoices.getSize());
        for (int i = 0; i < actualChoices.getSize(); i++) {
            // Set the choice to 0x01 if the sender should switch the 0 and 1 messages
            if (actualChoices.getBit(i, false)
                    ^ randomChoices.getBit(offset+i, false)) {
                switchBits.setBit(i, true, false);
            }
        }
        return switchBits;
    }
}
