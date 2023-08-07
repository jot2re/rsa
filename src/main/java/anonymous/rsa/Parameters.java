package anonymous.rsa;

public class Parameters implements Cloneable {
    private final int primeBits;
    private final int statBits;
    private final boolean jni;

    public Parameters(int primeBits, int statBits)  {
        this(primeBits, statBits, false);
    }

    public Parameters(int primeBits, int statBits, boolean jni)  {
        this.statBits = statBits;
        this.primeBits = primeBits;
        this.jni = jni;
    }

    public int getPrimeBits() {
        return primeBits;
    }

    public int getStatBits() {
        return statBits;
    }

    public boolean isJni() {
        return jni;
    }

    @Override
    public Object clone()  {
        return new Parameters(primeBits ,statBits);
    }
}
