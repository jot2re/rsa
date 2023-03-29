package dk.jot2re.mult.gilboa.ot.otextension;

import dk.jot2re.mult.gilboa.cointossing.CoinTossing;
import dk.jot2re.mult.gilboa.helper.HelperForTests;
import dk.jot2re.mult.gilboa.ot.base.DummyOt;
import dk.jot2re.network.INetwork;

import java.nio.ByteBuffer;
import java.security.SecureRandom;

public class OtExtensionTestContext {
  private final INetwork network;
  private final int myId;
  private final int otherId;
  private final int kbitLength;
  private final int lambdaSecurityParam;
  private final RotList seedOts;

  /**
   * Initialize the test context using specific parameters.
   *
   * @param myId
   *          The ID of the calling party
   * @param otherId
   *          The ID of the other party
   * @param kbitLength
   *          The computational security parameter
   * @param lambdaSecurityParam
   *          The statistical security parameter
   */
  public OtExtensionTestContext(int myId, int otherId, int kbitLength,
      int lambdaSecurityParam, INetwork network) {
    this.network = network;
    DummyOt dummyOt = new DummyOt(otherId, network);
    SecureRandom rand = new SecureRandom(HelperForTests.seedOne);
    this.seedOts = new RotList(rand, kbitLength);
    if (myId < otherId) {
      this.seedOts.send(dummyOt);
      this.seedOts.receive(dummyOt);
    } else {
      this.seedOts.receive(dummyOt);
      this.seedOts.send(dummyOt);
    }
    this.myId = myId;
    this.otherId = otherId;
    this.kbitLength = kbitLength;
    this.lambdaSecurityParam = lambdaSecurityParam;
  }

  public INetwork getNetwork() {
    return network;
  }

  /**
   * Creates a new OT extension resource pool based on a specific instance ID
   * and initializes necessary functionalities. This means it initializes coin
   * tossing using a randomness generator unique for {@code instanceId}.
   *
   * @param instanceId
   *          The id of the instance we wish to create a resource pool for
   * @return A new resources pool
   */
  public OtExtensionResourcePool createResources(int instanceId) {
    SecureRandom rand = createRand(instanceId);
    CoinTossing ct = new CoinTossing(myId, otherId, rand);
    ct.initialize(network);
    return new OtExtensionResourcePoolImpl(myId, otherId, kbitLength,
        lambdaSecurityParam, instanceId, rand, network, ct, seedOts);
  }

  /**
   * Creates a new randomness generator unique for {@code instanceId}.
   *
   * @param instanceId
   *          The ID which we wish to base the randomness generator on.
   * @return A new randomness generator unique for {@code instanceId}
   */
  public SecureRandom createRand(int instanceId) {
    ByteBuffer idBuffer = ByteBuffer.allocate(HelperForTests.seedOne.length + Integer.BYTES);
    byte[] seedBytes = idBuffer.putInt(instanceId).put(HelperForTests.seedOne).array();
    return new SecureRandom(seedBytes);
  }
}
