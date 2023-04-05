package dk.jot2re.mult.gilboa;

import dk.jot2re.mult.gilboa.ot.otextension.*;
import dk.jot2re.mult.gilboa.util.AesCtrDrbg;
import dk.jot2re.mult.gilboa.util.Drbg;
import dk.jot2re.mult.gilboa.util.StrictBitVector;
import dk.jot2re.network.INetwork;

import java.math.BigInteger;
import java.util.List;

public class GilboaOTFactory {
    private GilboaOTSender sender = null;
    private GilboaOTReceiver receiver = null;
    private final RotFactory rot;
    private final OtExtensionResourcePool resources;
    private final INetwork network;
    private final int batchSize;

    public GilboaOTFactory(RotFactory randomOtExtension, OtExtensionResourcePool resources,
                            INetwork network, int batchSize) {
        this.rot = randomOtExtension;
        this.resources = resources;
        this.network = network;
        this.batchSize = batchSize;
    }

    public void initSender() {
        if (this.sender == null) {
            RotSender sender = rot.createSender();
            this.sender = new GilboaOTSender(sender, resources, network, batchSize);
            // Make an initial batch
            this.sender.makeBatch();
        }
    }
    public void initReceiver() {
        if (this.receiver == null) {
            RotReceiver receiver = rot.createReceiver();
            this.receiver = new GilboaOTReceiver(receiver, resources, network,
                    batchSize);
            // Make an initial batch
            this.receiver.makeBatch();
        }
    }

    public List<BigInteger> send(BigInteger value, BigInteger modulo) {
        return this.sender.send(value, modulo);
    }

    public List<BigInteger> receive(StrictBitVector actualChoices, int amountBytes) {
        return receiver.receive(actualChoices, amountBytes);
    }

    protected static byte[] expand(byte[] inputBits, int amountBytes) {
        Drbg rng = new AesCtrDrbg(inputBits);
        byte[] res = new byte[amountBytes];
        rng.nextBytes(res);
        return res;
    }
}
