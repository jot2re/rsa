package dk.jot2re.mult;

import java.io.Serializable;
import java.math.BigInteger;

public class ShamirShare implements IShare {
    private final BigInteger value;
    public ShamirShare(final BigInteger value) {
        this.value = value;
    }
    public ShamirShare(final BigInteger value, final BigInteger modulo) {
        if (modulo != null) {
            this.value = value.mod(modulo);
        } else {
            this.value = value;
        }
    }
    @Override
    public Serializable getRawShare() {
        return null;
    }
}
