package anonymous.mult.ot.ips;

import anonymous.mult.ot.ot.otextension.OtExtensionResourcePool;
import anonymous.mult.ot.ot.otextension.RotReceiver;
import anonymous.mult.ot.util.*;
import anonymous.network.INetwork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class IPSOTReceiver {
    private static final Logger logger = LoggerFactory.getLogger(IPSOTReceiver.class);
    private int amount;
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

    public IPSOTReceiver(RotReceiver rotReceiver,
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

    // TODO based on statistical sec and modulo we can calculate what expansion size to use
    public List<BigInteger> receive(BigInteger value, BigInteger modulo, int expansionSizeBytes) {
        amount = modulo.bitLength() + resources.getLambdaSecurityParam();
        // Check if there is still an unused random OT stored, if not, execute a
        // random OT extension
        if (offset < 0 || offset+amount >= batchSize) {
            makeBatch();
        }
        Pair<ArrayList<BigInteger>, ArrayList<BigInteger>> uValues = sampleSharing(value, modulo);
        network.send(resources.getOtherId(), uValues.getFirst());
        network.send(resources.getOtherId(), uValues.getSecond());
        ArrayList<byte[]> adjustments = network.receive(resources.getOtherId());
        if (adjustments.size() != amount) {
            throw new MaliciousException("Unexpected adjustment size");
        }
        List<BigInteger> shares = new ArrayList<>(amount);
        for (int i = 0; i < amount; i++) {
            byte[] message = randomMessages.get(offset+i).toByteArray();
            byte[] messageExpanded = IPSOTFactory.expand(message, expansionSizeBytes);
            if (randomChoices.getBit(offset+i, false)) {
                ByteArrayHelper.xor(messageExpanded, adjustments.get(i));
            }
            shares.add(new BigInteger(1, messageExpanded));
        }
        offset += amount;
        return shares;
    }

    private Pair<ArrayList<BigInteger>, ArrayList<BigInteger>> sampleSharing(BigInteger value, BigInteger modulo) {
        Drng rnd = new DrngImpl(resources.getRandomGenerator());
        ArrayList<BigInteger> uZero = new ArrayList<>(amount);
        ArrayList<BigInteger> uOne = new ArrayList<>(amount);
        BigInteger partialChoiceSum = BigInteger.ZERO;
        for (int i = 0; i < amount-1; i++) {
            BigInteger currentZeroChoice = rnd.nextBigInteger(modulo);
            BigInteger currentOneChoice = rnd.nextBigInteger(modulo);
            if (randomChoices.getBit(offset+i, false)) {
                partialChoiceSum = partialChoiceSum.add(currentOneChoice);
            } else {
                partialChoiceSum = partialChoiceSum.add(currentZeroChoice);
            }
            uZero.add(currentZeroChoice);
            uOne.add(currentOneChoice);
        }
        // Add last sums to make sure things fit
        BigInteger delta = value.subtract(partialChoiceSum).mod(modulo);
        if (randomChoices.getBit(offset+amount-1, false)) {
            uZero.add(rnd.nextBigInteger(modulo));
            uOne.add(delta);
        } else {
            uZero.add(delta);
            uOne.add(rnd.nextBigInteger(modulo));
        }
        return new Pair<>(uZero, uOne);
    }
}
