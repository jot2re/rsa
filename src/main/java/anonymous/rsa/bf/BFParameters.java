package anonymous.rsa.bf;

import anonymous.mult.IMult;
import anonymous.rsa.Parameters;

public class BFParameters extends Parameters {
    private final IMult mult;

    public BFParameters(Parameters baseParameters, IMult mult) {
        super(baseParameters.getPrimeBits(), baseParameters.getStatBits());
        this.mult = mult;
    }
    public BFParameters(int primeBits, int statBits, IMult mult) {
        this(primeBits, statBits, mult, false);
    }

    public BFParameters(int primeBits, int statBits, IMult mult, boolean jni) {
        super(primeBits, statBits, jni);
        this.mult = mult;
    }

    public IMult getMult() {
        return mult;
    }
}
