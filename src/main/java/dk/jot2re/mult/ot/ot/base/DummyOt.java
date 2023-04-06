package dk.jot2re.mult.ot.ot.base;


import dk.jot2re.mult.ot.util.StrictBitVector;
import dk.jot2re.network.INetwork;

/**
 * Implementation for testing and proof-of-concept protocols. This class carries
 * out an INSECURE OT.
 */
public class DummyOt implements Ot {

  private final int otherId;
  private final INetwork network;

  /**
   * Construct an insecure dummy OT object based on a real network.
   *
   * @param otherId
   *          The ID of the other party
   * @param network
   *          The network to use
   */
  public DummyOt(int otherId, INetwork network) {
    this.otherId = otherId;
    this.network = network;
  }

  @Override
  public StrictBitVector receive(boolean choiceBit) {
    byte[] messageZeroRaw = this.network.receive(this.otherId);
    byte[] messageOneRaw = this.network.receive(this.otherId);
    return !choiceBit
        ? new StrictBitVector(messageZeroRaw)
        : new StrictBitVector(messageOneRaw);
  }

  @Override
  public void send(StrictBitVector messageZero, StrictBitVector messageOne) {
    this.network.send(otherId, messageZero.toByteArray());
    this.network.send(otherId, messageOne.toByteArray());
  }
}
