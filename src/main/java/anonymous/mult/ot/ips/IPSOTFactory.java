package anonymous.mult.ot.ips;

import anonymous.mult.ot.ot.otextension.OtExtensionResourcePool;
import anonymous.mult.ot.ot.otextension.RotFactory;
import anonymous.mult.ot.ot.otextension.RotReceiver;
import anonymous.mult.ot.ot.otextension.RotSender;
import anonymous.mult.ot.util.AesCtrDrbg;
import anonymous.network.INetwork;
import anonymous.mult.ot.util.Drbg;

public class IPSOTFactory {
    private IPSOTSender sender = null;
    private IPSOTReceiver receiver = null;
    private final RotFactory rot;
    private final OtExtensionResourcePool resources;
    private final INetwork network;
    private final int batchSize;

    public IPSOTFactory(RotFactory randomOtExtension, OtExtensionResourcePool resources,
                        INetwork network, int batchSize) {
        this.rot = randomOtExtension;
        this.resources = resources;
        this.network = network;
        this.batchSize = batchSize;
    }

    public void initSender() {
        if (this.sender == null) {
            RotSender sender = rot.createSender();
            this.sender = new IPSOTSender(sender, resources, network, batchSize);
            // Make an initial batch
            this.sender.makeBatch();
        }
    }
    public void initReceiver() {
        if (this.receiver == null) {
            RotReceiver receiver = rot.createReceiver();
            this.receiver = new IPSOTReceiver(receiver, resources, network,
                    batchSize);
            // Make an initial batch
            this.receiver.makeBatch();
        }
    }

    public IPSOTSender getSender() {
        return sender;
    }

    public IPSOTReceiver getReceiver() {
        return receiver;
    }

    protected static byte[] expand(byte[] inputBits, int amountBytes) {
        Drbg rng = new AesCtrDrbg(inputBits);
        byte[] res = new byte[amountBytes];
        rng.nextBytes(res);
        return res;
    }
}
