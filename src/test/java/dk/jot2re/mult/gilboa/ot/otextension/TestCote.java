package dk.jot2re.mult.gilboa.ot.otextension;


import dk.jot2re.mult.gilboa.cointossing.CoinTossing;
import dk.jot2re.mult.gilboa.helper.HelperForTests;
import dk.jot2re.mult.gilboa.util.StrictBitVector;
import dk.jot2re.network.INetwork;
import dk.jot2re.network.PlainNetwork;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.security.SecureRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestCote {
  private final int kbitSecurity = 128;
  private CoteFactory cote;
  private SecureRandom rand;
  private INetwork network;

  /**
   * Setup a correlated OT functionality.
   */
  @BeforeEach
  public void setup() throws NoSuchFieldException, SecurityException,
      IllegalArgumentException, IllegalAccessException {
    rand = new SecureRandom(HelperForTests.seedOne);
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
        128, 40, 1, rand, ct, seedOts);
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
