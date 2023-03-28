package dk.jot2re.mult.gilboa.ot.otextension;


import dk.jot2re.network.INetwork;

/**
 * Abstract factory for a protocol instance of random OT extension.
 */
public class RotFactory {

  private final OtExtensionResourcePool resources;
  private final INetwork network;

  /**
   * Constructs a new random OT protocol and constructs the internal sender and
   * receiver objects.
   *
   * @param resources The common resource pool for OT extension
   * @param network The network instance
   */
  public RotFactory(OtExtensionResourcePool resources, INetwork network) {
    this.resources = resources;
    this.network = network;
  }

  public RotSender createSender() {
    CoteSender sender = new CoteSender(resources, network);
    return new RotSenderImpl(sender, resources, network);
  }

  public RotReceiver createReceiver() {
    CoteReceiver receiver = new CoteReceiver(resources, network);
    return new RotReceiverImpl(receiver, resources, network);
  }
}
