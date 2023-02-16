package dk.jot2re.rsa.bf;

import dk.jot2re.network.INetwork;
import dk.jot2re.rsa.Parameters;

import java.security.SecureRandom;

public class BFParameters extends Parameters {
    public BFParameters(Parameters baseParameters) {
        super(baseParameters.getPrimeBits(), baseParameters.getStatBits(), baseParameters.getNetwork(), baseParameters.getRandom());
    }
    public BFParameters(int primeBits, int statBits, INetwork network, SecureRandom random) {
        super(primeBits, statBits, network, random);
    }
}
