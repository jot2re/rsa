package anonymous.mult.shamir;

public class ShamirResourcePool {
    private final int myId;
    private final int parties;
    private final int compSec;
    private final int statSec;

    public ShamirResourcePool(int myId, int parties,  int compSec, int statSec) {
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

    public int getThreshold() {
        return (getParties()-1)/2;
    }
}
