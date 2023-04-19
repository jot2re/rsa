package dk.jot2re.mult;

import java.math.BigInteger;

public abstract class AbstractAdditiveMult implements IMult<BigInteger> {

    @Override
    public BigInteger share(BigInteger value, BigInteger modulo) {
        return value.mod(modulo);
    }

    @Override
    public BigInteger combine(BigInteger share, BigInteger modulo) {
        return share.mod(modulo);
    }

    @Override
    public abstract BigInteger multShares(BigInteger left, BigInteger right, BigInteger modulo);
}
