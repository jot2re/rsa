package dk.jot2re.mult.replicated;

public class ReplictedMultResourcePool {
    private final int myId;
    private final int parties;
    private final int compSec;
    private final int statSec;

    public ReplictedMultResourcePool(int myId, int parties,  int compSec, int statSec) {
        this.myId = myId;
        this.parties = parties;
        this.compSec = compSec;
        this.statSec = statSec;
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
}
