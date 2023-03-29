package dk.jot2re.mult.gilboa.oracle;

import java.nio.ByteBuffer;

import static dk.jot2re.mult.gilboa.oracle.Util.ceil;


/**
 * A random oracle based PRG.
 */
public class Prg {
    private long ctr = 0L;
    private final RandomOracle oracle;

    /**
     * Initialize a prg with a specific seed.
     * @param seed the seed to use in the prg
     */
    public Prg(byte[] seed) {
        this.oracle = new RandomOracle(seed, Parameters.HMAC_BYTES_OUTPUT);
    }

    /**
     * Return a certain amount of pseudorandom bytes from the prg.
     * @param amount the amount of bytes to return
     * @return the random bytes
     */
    public byte[] getBytes(int amount) {
        if (amount < 1) {
            throw new RuntimeException("Amount of bytes must be positive");
        }
        byte[] res = new byte[amount];
        int position = 0;
        byte[] buffer;
        for (int i = 0; i < ceil(amount, Parameters.HMAC_BYTES_OUTPUT); i++) {
            byte[] ctrBytes = ByteBuffer.allocate(Long.BYTES).putLong(ctr).array();
            buffer = oracle.apply(ctrBytes);
            System.arraycopy(buffer, 0, res, position, Math.min(Parameters.HMAC_BYTES_OUTPUT, amount-position));
            position = position + Parameters.HMAC_BYTES_OUTPUT;
            ctr++;
        }
        return res;
    }

}
