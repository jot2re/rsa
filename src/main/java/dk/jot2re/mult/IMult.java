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
        return combineToAdditive(multShares(shareFromAdditive(shareA, modulo), shareFromAdditive(shareB, modulo), modulo), modulo);
    }

    /**
     * Shares a private value from a party
     * @param value
     * @param modulo
     * @return
     */
    T share(BigInteger value, BigInteger modulo);

    /**
     * Receive a private value from a party
     * @param partyId
     * @param modulo
     * @return
     */
    T share(int partyId, BigInteger modulo);

    /**
     * Shares a value that is already additive shared.
     * Method assumes that each party calls this on their additive share
     * @param value
     * @return
     */
    T shareFromAdditive(BigInteger value, BigInteger modulo);

    /**
     * Combine shares into an additive sharing
     * @param share
     * @return
     */
    BigInteger combineToAdditive(T share, BigInteger modulo);
    T multShares(T left, T right, BigInteger modulo);
}
