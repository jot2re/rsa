package dk.jot2re.rsa;

public class Parameters implements Cloneable {
    private final int primeBits;
    private final int statBits;

    public Parameters(int primeBits, int statBits)  {
        this.statBits = statBits;
        this.primeBits = primeBits;
    }

    public int getPrimeBits() {
        return primeBits;
    }

    public int getStatBits() {
        return statBits;
    }

    @Override
    public Object clone()  {
        return new Parameters(primeBits ,statBits);
    }
}
