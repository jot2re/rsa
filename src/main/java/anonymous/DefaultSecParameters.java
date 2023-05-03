package anonymous;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class DefaultSecParameters {
    public static final String DIGEST_ALGO = "SHA-256";
    public static final boolean REJECTION_SAMPLING = true;
    public static final int COMP_SEC = 256;
    public static final int STAT_SEC = 64;
    public static final int PRIME_BITLENGTH = 1024;
    public static final int MODULO_BITLENGTH =2*PRIME_BITLENGTH;
    public static final BigInteger MODULO = findMaxPrime(PRIME_BITLENGTH);

    private static BigInteger findMaxPrime(int bitLength) {
        BigInteger candidate = BigInteger.TWO.pow(MODULO_BITLENGTH).subtract(BigInteger.ONE);
        while (!candidate.isProbablePrime(STAT_SEC)) {
            candidate = candidate.subtract(BigInteger.TWO);
        }
        return candidate;
    }

    public static MessageDigest getDigest() {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance(DIGEST_ALGO);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(DIGEST_ALGO + " not supported", e);
        }
        return digest;
    }
}
