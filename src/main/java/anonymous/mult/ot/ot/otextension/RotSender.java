package anonymous.mult.ot.ot.otextension;

import anonymous.mult.ot.util.Pair;
import anonymous.mult.ot.util.StrictBitVector;

import java.util.List;

/**
 * Protocol class for the party acting as the sender in a random OT extension.
 */
public interface RotSender {

  /**
   * Constructs a new batch of random OTs.
   *
   * @param size
   *          The amount of random OTs to construct
   * @return A pair of lists of StrictBitVectors. First list consists of the
   *         choice-zero messages. Second list consists of the choice-one
   *         messages
   */
  Pair<List<StrictBitVector>, List<StrictBitVector>> extend(int size);
}
