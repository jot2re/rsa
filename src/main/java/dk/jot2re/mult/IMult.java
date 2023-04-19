package dk.jot2re.mult;

import dk.jot2re.network.INetwork;

import java.math.BigInteger;

public interface IMult<T extends IShare> {
    void init(INetwork network);

    /**
     * Take additive shares as input from each party and return an additive share of the product.
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

    T share(BigInteger value, BigInteger modulo);

    T share(int partyId, BigInteger modulo);

    BigInteger open(T share, BigInteger modulo);

//    IShare add(IShare left, IShare right);
//
//    IShare add(IShare share, BigInteger known);

    T multShares(T left, T right, BigInteger modulo);

    T multConst(T share, BigInteger known, BigInteger modulo);
}
