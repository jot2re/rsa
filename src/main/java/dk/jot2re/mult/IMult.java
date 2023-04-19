package dk.jot2re.mult;

import dk.jot2re.network.INetwork;

import java.io.Serializable;
import java.math.BigInteger;

public interface IMult<T extends Serializable> {
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
    default BigInteger mult(BigInteger shareA, BigInteger shareB, BigInteger modulo, int upperBound) {
        if (shareA == null || shareB == null || modulo == null) {
            throw new NullPointerException("Input for multiplication as to be non-null");
        }
        return combine(multShares(share(shareA, modulo), share(shareB, modulo), modulo), modulo);
    }

    /**
     * Shares a value that is already additive shared
     * @param value
     * @return
     */
    T share(BigInteger value, BigInteger modulo);

    /**
     * Combine shares into an additive sharing
     * @param share
     * @return
     */
    BigInteger combine(T share, BigInteger modulo);
    T multShares(T left, T right, BigInteger modulo);
}
