package dk.jot2re.rsa.bf;

import dk.jot2re.mult.IMult;
import dk.jot2re.network.INetwork;
import dk.jot2re.rsa.Parameters;

import java.security.SecureRandom;

public class BFParameters extends Parameters {
    private final IMult mult;

    public BFParameters(Parameters baseParameters, IMult mult) {
        super(baseParameters.getPrimeBits(), baseParameters.getStatBits(), baseParameters.getNetwork(), baseParameters.getRandom());
        this.mult = mult;
    }
    public BFParameters(int primeBits, int statBits, INetwork network, IMult mult, SecureRandom random) {
        super(primeBits, statBits, network, random);
        this.mult = mult;
    }

    public IMult getMult() {
        return mult;
    }
}
