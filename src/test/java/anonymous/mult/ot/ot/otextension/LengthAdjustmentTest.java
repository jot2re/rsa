package anonymous.mult.ot.ot.otextension;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

public class LengthAdjustmentTest {

  @Test
  public void testAdjustLongCandidate() {
    final int candidateLength = 10;
    final int adjustedLength = 5;
    testLongerOrEqualCandidateLength(candidateLength, adjustedLength);
  }

  @Test
  public void testAdjustEqualLengthCandidate() {
    final int candidateLength = 5;
    final int adjustedLength = 5;
    testLongerOrEqualCandidateLength(candidateLength, adjustedLength);
  }

  @Test
  public void testZeroLength() {
    final int candidateLength = 0;
    final int adjustedLength = 0;
    testLongerOrEqualCandidateLength(candidateLength, adjustedLength);
  }

  @Test
  public void testZeroLength2() {
    final int candidateLength = 10;
    final int adjustedLength = 0;
    testLongerOrEqualCandidateLength(candidateLength, adjustedLength);
  }

  private void testLongerOrEqualCandidateLength(final int candidateLength,
      final int adjustedLength) {
    byte[] candidate = new byte[candidateLength];
    new Random().nextBytes(candidate);
    byte[] adjusted = LengthAdjustment.adjust(candidate, adjustedLength);
    assertArrayEquals(Arrays.copyOf(candidate, adjustedLength), adjusted);
  }

  @Test
  public void testAdjustShortCandidate() {
    final int candidateLength = 5;
    final int adjustedLength = 10;
    testShorterCandidateLength(candidateLength, adjustedLength);
  }

  @Test
  public void testAdjustLongStrech() {
    final int candidateLength = 5;
    final int adjustedLength = 32;
    testShorterCandidateLength(candidateLength, adjustedLength);
  }

  @Test
  public void testAdjustEmptyCandidate() {
    final int candidateLength = 0;
    final int adjustedLength = 10;
    testShorterCandidateLength(candidateLength, adjustedLength);
  }

  @Test
  public void testAdjustNegativeLength() {
    final int candidateLength = 5;
    final int adjustedLength = -10;
    assertThrows(IllegalArgumentException.class, ()-> testShorterCandidateLength(candidateLength, adjustedLength));
  }

  @Test
  public void testAdjustNullCandidate() {
    assertThrows(NullPointerException.class, ()-> LengthAdjustment.adjust(null, 10));
  }

  private void testShorterCandidateLength(final int candidateLength, final int adjustedLength) {
    byte[] candidate = new byte[candidateLength];
    new Random().nextBytes(candidate);
    byte[] adjusted1 = LengthAdjustment.adjust(candidate, adjustedLength);
    assertEquals(adjustedLength, adjusted1.length);
    byte[] adjusted2 = LengthAdjustment.adjust(candidate, adjustedLength);
    assertArrayEquals(adjusted1, adjusted2);
    assertFalse(Arrays.equals(new byte[adjusted1.length], adjusted1));
  }

}
