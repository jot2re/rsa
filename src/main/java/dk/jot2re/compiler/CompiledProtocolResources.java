package dk.jot2re.compiler;

public class CompiledProtocolResources {
    private final int compSec;
    public CompiledProtocolResources(int compSec) {
        if ((compSec & 8) != 0) {
            throw new RuntimeException("Must be product of 8");
        }
        this.compSec = compSec;
    }

    public int getCompSec() {
        return compSec;
    }

    public int getCompSecBytes() {
        return compSec / 8;
    }
}
