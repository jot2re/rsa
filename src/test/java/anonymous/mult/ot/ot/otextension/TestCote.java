package anonymous.mult.ot.ot.otextension;


import anonymous.mult.ot.cointossing.CoinTossing;
import anonymous.mult.ot.helper.HelperForTests;
import anonymous.mult.ot.util.AesCtrDrbg;
import anonymous.network.PlainNetwork;
import anonymous.mult.ot.util.Drbg;
import anonymous.mult.ot.util.StrictBitVector;
import anonymous.network.INetwork;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestCote {
  private final int kbitSecurity = 128;
  private CoteFactory cote;
  private Drbg rand;
  private INetwork network;

  /**
   * Setup a correlated OT functionality.
   */
  @BeforeEach
  public void setup() throws NoSuchFieldException, SecurityException,
      IllegalArgumentException, IllegalAccessException {
    rand = new AesCtrDrbg(HelperForTests.seedOne);
    // fake network
    network = new PlainNetwork<>(0, 2, 0, null);
    RotList seedOts = new RotList(rand, kbitSecurity);
    Field sent = RotList.class.getDeclaredField("sent");
    sent.setAccessible(true);
    sent.set(seedOts, true);
    Field received = RotList.class.getDeclaredField("received");
    received.setAccessible(true);
    received.set(seedOts, true);
    CoinTossing ct = new CoinTossing(1, 2, rand);
    OtExtensionResourcePool resources = new OtExtensionResourcePoolImpl(1, 2,
        128, 40, 1, rand, network, ct, seedOts);
    this.cote = new CoteFactory(resources, network);
  }

  /**** NEGATIVE TESTS. ****/
  @Test
  public void testIllegalExtendSender() {
    boolean thrown = false;
    try {
      cote.getSender().extend(127);
    } catch (IllegalArgumentException e) {
      assertEquals("The amount of OTs must be a positive integer divisible by 8",
          e.getMessage());
      thrown = true;
    }
    assertEquals(true, thrown);
    thrown = false;
    try {
      cote.getSender().extend(-1);
    } catch (IllegalArgumentException e) {
      assertEquals("The amount of OTs must be a positive integer",
          e.getMessage());
      thrown = true;
    }
    assertEquals(true, thrown);
    thrown = false;
    try {
      cote.getSender().extend(0);
    } catch (IllegalArgumentException e) {
      assertEquals("The amount of OTs must be a positive integer",
          e.getMessage());
      thrown = true;
    }
    assertEquals(true, thrown);
  }

  @Test
  public void testIllegalExtendReceiver() {
    boolean thrown = false;
    try {
      StrictBitVector choices = new StrictBitVector(0);
      cote.getReceiver().extend(choices);
    } catch (IllegalArgumentException e) {
      assertEquals("The amount of OTs must be a positive integer",
          e.getMessage());
      thrown = true;
    }
    assertEquals(true, thrown);
  }

  @Test
  public void testGetReceiverTwice() {
    CoteReceiver r1 = cote.getReceiver();
    CoteReceiver r2 = cote.getReceiver();
    assertEquals(r1, r2);
  }

}
