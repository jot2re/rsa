package dk.jot2re.rsa.bf;

import dk.jot2re.mult.IMult;
import dk.jot2re.rsa.Parameters;

public class BFParameters extends Parameters {
    private final IMult mult;

    public BFParameters(Parameters baseParameters, IMult mult) {
        super(baseParameters.getPrimeBits(), baseParameters.getStatBits());
        this.mult = mult;
    }
    public BFParameters(int primeBits, int statBits, IMult mult) {
        super(primeBits, statBits);
        this.mult = mult;
    }

    public IMult getMult() {
        return mult;
    }
}
