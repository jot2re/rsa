package dk.jot2re.mult.replicated;

import dk.jot2re.mult.ot.util.Drng;

public class ReplictedMultResourcePool {
    private final int myId;
    private final int parties;
    private final int compSec;
    private final int statSec;
    private final Drng rng;

    public ReplictedMultResourcePool(int myId, int parties,  int compSec, int statSec, Drng drng) {
        this.myId = myId;
        this.parties = parties;
        this.compSec = compSec;
        this.statSec = statSec;
        this.rng = drng;
    }

    public int getMyId() {
        return myId;
    }

    public int getParties() {
        return parties;
    }

    public int getCompSec() {
        return compSec;
    }

    public int getStatSec() {
        return statSec;
    }

    public Drng getRng() {
        return rng;
    }
}
