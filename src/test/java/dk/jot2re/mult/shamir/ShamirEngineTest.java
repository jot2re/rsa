package dk.jot2re.mult.shamir;

import dk.jot2re.mult.ot.util.AesCtrDrbg;
import dk.jot2re.mult.ot.util.Drng;
import dk.jot2re.mult.ot.util.DrngImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ShamirEngineTest {
    private static ShamirResourcePool DEFAULT_RESOURCE;
    private static ShamirEngine DEFAULT_ENGINE;
    private static BigInteger DEFAULT_MODULO;

    @BeforeAll
    public static void setup() {
        DEFAULT_RESOURCE = getResourcePool(0, 3, 128, 40);
        DEFAULT_ENGINE = new ShamirEngine(DEFAULT_RESOURCE);
        DEFAULT_MODULO = BigInteger.probablePrime(DEFAULT_RESOURCE.getCompSec(), new Random(0));
    }

    public static ShamirResourcePool getResourcePool(int myId, int parties, int compSec, int statSec) {
        byte[] seed = new byte[32];
        seed[0] = (byte) myId;
        Drng rand = new DrngImpl(new AesCtrDrbg(seed));
        return new ShamirResourcePool(myId, parties, compSec, statSec, rand);
    }

    @Test
    void sunshineSharing() {
        Map<Integer, BigInteger> shares = DEFAULT_ENGINE.share(BigInteger.valueOf(42), DEFAULT_MODULO);
        List<BigInteger> toCombine = new ArrayList<>();
        for (int i = 1; i < DEFAULT_RESOURCE.getParties(); i++) {
            toCombine.add(shares.get(i));
        }
        BigInteger res = DEFAULT_ENGINE.combine(toCombine, DEFAULT_MODULO);
        assertEquals(BigInteger.valueOf(42), res);
    }
}
