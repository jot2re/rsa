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
    default BigInteger mult(BigInteger shareA, BigInteger shareB, BigInteger modulo) {
        return mult(shareA, shareB, modulo, modulo.bitLength());
    }

    /**
     * Potentially optimized method if arguments are bounded
     * @param shareA The additive share of a number A
     * @param shareB The additive share of a number B
     * @param upperBound The max amount of bits used to represent any share to be multiplied
     * @return A share of the product of A*B
     */
    BigInteger mult(BigInteger shareA, BigInteger shareB, BigInteger modulo, int upperBound);
}
