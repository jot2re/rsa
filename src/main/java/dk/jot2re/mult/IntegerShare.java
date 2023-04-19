package dk.jot2re.mult;

import java.math.BigInteger;

public class IntegerShare implements IShare<BigInteger> {
    private final BigInteger value;
    public IntegerShare(final BigInteger value) {
        this.value = value;
    }
    public IntegerShare(final BigInteger value, final BigInteger modulo) {
        if (modulo != null) {
            this.value = value.mod(modulo);
        } else {
            this.value = value;
        }
    }

    @Override
    public BigInteger getRawShare() {
        return value;
    }
}
