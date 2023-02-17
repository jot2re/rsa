package dk.jot2re.mult;

import dk.jot2re.network.INetwork;

import java.math.BigInteger;

public interface IMult {
    void init(INetwork network);

    /**
     * @param shareA The additive share of a number A
     * @param shareB The additive share of a number B
     * @return A share of the product of A*B
     */
    BigInteger mult(BigInteger shareA, BigInteger shareB, BigInteger modulo);
}
