package dk.jot2re.rsa.bf.dto;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;

public class Phase1Pivot implements Serializable {
    private final ArrayList<BigInteger> gammas;
    private final ArrayList<BigInteger> nuShares;
    public Phase1Pivot() {
        gammas = new ArrayList<>();
        nuShares = new ArrayList<>();
    }

    public void addElements(BigInteger gamma, BigInteger nuShare) {
        gammas.add(gamma);
        nuShares.add(nuShare);
    }

    public ArrayList<BigInteger> getGammas() {
        return gammas;
    }

    public ArrayList<BigInteger> getNuShares() {
        return nuShares;
    }
}
