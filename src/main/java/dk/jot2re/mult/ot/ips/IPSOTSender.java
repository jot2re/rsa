package dk.jot2re.mult.ot.ips;

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

public class IPSOTSender {
    private static final Logger logger = LoggerFactory.getLogger(IPSOTSender.class);
    private final int amount;
    private final RotSender sender;
    private final OtExtensionResourcePool resources;
    private final INetwork network;
    private final int batchSize;

    // The random messages generated in the underlying random OT functionality
    private Pair<List<StrictBitVector>, List<StrictBitVector>> randomMessages;
    // Index of the current random OT to use
    private int offset = -1;

    public IPSOTSender(RotSender rotSender, OtExtensionResourcePool resources, INetwork network,
                       int batchSize) {
        this.sender = rotSender;
        this.resources = resources;
        this.network = network;
        this.batchSize = batchSize;
        this.amount = resources.getComputationalSecurityParameter()+resources.getLambdaSecurityParam();
    }

    public void makeBatch() {
        logger.info("Making a new batch of OTs...");
        randomMessages = sender.extend(batchSize);
        offset = 0;
    }

    public List<BigInteger> send(BigInteger value, BigInteger modulo, int expansionSizeBytes) {
        // Check if there is still an unused random OT stored, if not, execute a
        // random OT extension
        if (offset < 0 || offset + amount >= batchSize) {
            makeBatch();
        }
        Pair<ArrayList<BigInteger>, ArrayList<BigInteger>> uValues = network.receive(resources.getOtherId());
        if (uValues.getFirst().size() != amount || uValues.getSecond().size() != amount) {
            throw new MaliciousException("Unexpected adjustment size");
        }
        ArrayList<byte[]> corrections = new ArrayList<>(amount);
        List<BigInteger> deltas = new ArrayList<>(amount);
        for (int i = 0; i < amount; i++) {
            byte[] randZeroBytes = IPSOTFactory.expand(randomMessages.getFirst().get(offset+i).toByteArray(), expansionSizeBytes);
            BigInteger randZero = new BigInteger(1, randZeroBytes);
            BigInteger delta = randZero.subtract(value.multiply(uValues.getFirst().get(i)));
            BigInteger oneMsg = value.multiply(uValues.getSecond().get(i)).add(delta).mod(modulo);
            byte[] oneAdjustment = moveToArray(oneMsg, expansionSizeBytes);
            byte[] randOneBytes = IPSOTFactory.expand(randomMessages.getSecond().get(offset+i).toByteArray(), expansionSizeBytes);
            ByteArrayHelper.xor(randOneBytes, oneAdjustment);
            corrections.add(randOneBytes);
            deltas.add(delta);
        }
        network.send(resources.getOtherId(), corrections);
        offset += amount;
        return deltas;
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
