package dk.jot2re.mult;

import java.math.BigInteger;
import java.util.ArrayList;

public class ReplicatedShare implements IShare {
    // TODO use array
    private final ArrayList<BigInteger> value;
    public ReplicatedShare(final ArrayList<BigInteger> value) {
        this.value = value;
    }

    @Override
    public ArrayList<BigInteger> getRawShare() {
        return value;
    }
}
