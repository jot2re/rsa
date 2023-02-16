package dk.jot2re.rsa.bf.dto;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;

public class Phase1Other implements Serializable {
    private final ArrayList<BigInteger> nuShares;

    public Phase1Other(ArrayList<BigInteger> nuShares) {
        this.nuShares = nuShares;
    }

    public Phase1Other() {
        nuShares = new ArrayList<>();
    }

    public void addElements(BigInteger nuShare) {
        nuShares.add(nuShare);
    }

    public ArrayList<BigInteger> getNuShares() {
        return nuShares;
    }
}
