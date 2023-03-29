package dk.jot2re.mult.gilboa.oracle;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import static dk.jot2re.mult.gilboa.oracle.Util.ceil;


/**
 * A random oracle class, realized using HMAC with SHA-256
 */
public class RandomOracle {
    private final int imageInBytes;
    private final int hmacApplications;
    private final Mac mac;

    /**
     * Initialize an oracle with imageInBytes amount of bytes in output.
     * @param seed the seed used to sample to oracle from a family
     * @param imageInBytes the amount of bytes to output from queries to the oracle
     */
    public RandomOracle(byte[] seed, int imageInBytes) {
        if (seed == null || imageInBytes < 1) {
            throw new RuntimeException("Invalid input");
        }
        this.imageInBytes = imageInBytes;
        this.hmacApplications = ceil(imageInBytes, Parameters.HMAC_BYTES_OUTPUT);
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(seed, Parameters.HMAC_ALGORITHM);
            this.mac = Mac.getInstance(Parameters.HMAC_ALGORITHM);
            this.mac.init(secretKeySpec);
        } catch (IllegalArgumentException | InvalidKeyException | NoSuchAlgorithmException e) {
            throw new RuntimeException("Could not initialize PRG", e);
        }
    }

    /**
     * Query the oracle.
     * @param input the input which to query the oracle on
     * @return a byte array of pseudorandom bytes
     */
    public byte[] apply(byte[] input) {
        if (input == null || input.length < 1) {
            throw new RuntimeException("Empty input");
        }
        byte[] res = new byte[imageInBytes];
        int position = 0;
        byte[] buffer;
        for (int i = 0; i < hmacApplications; i++) {
            mac.reset();
            byte[] ctrBytes = ByteBuffer.allocate(Long.BYTES).putLong(i).array();
            mac.update(ctrBytes);
            buffer = mac.doFinal(input);
            System.arraycopy(buffer, 0, res, position, Math.min(Parameters.HMAC_BYTES_OUTPUT, imageInBytes-position));
            position++;
        }
        return res;
    }
}
