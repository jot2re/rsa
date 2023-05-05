package anonymous.mult.shamir;

import anonymous.mult.ot.util.AesCtrDrbg;
import anonymous.mult.ot.util.Drng;
import anonymous.mult.ot.util.DrngImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.*;

import static anonymous.DefaultSecParameters.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ShamirEngineTest {
    private static ShamirResourcePool DEFAULT_RESOURCE;
    private static ShamirEngine DEFAULT_ENGINE;

    @BeforeAll
    public static void setup() {
        DEFAULT_RESOURCE = getResourcePool(0, 3, COMP_SEC, STAT_SEC);
        DEFAULT_ENGINE = new ShamirEngine(DEFAULT_RESOURCE.getParties(), new Random(42));
    }

    public static ShamirResourcePool getResourcePool(int myId, int parties, int compSec, int statSec) {
        byte[] seed = new byte[32];
        seed[0] = (byte) myId;
        Drng rand = new DrngImpl(new AesCtrDrbg(seed));
        return new ShamirResourcePool(myId, parties, compSec, statSec);
    }

    @Test
    void sunshineSharing() {
        Map<Integer, BigInteger> shares = DEFAULT_ENGINE.share(BigInteger.valueOf(42), MODULO);
        Map<Integer, BigInteger> toCombine = new HashMap<>();
        for (int i = 0; i < DEFAULT_RESOURCE.getParties(); i++) {
            toCombine.put(i, shares.get(i));
        }
        BigInteger res = DEFAULT_ENGINE.combine(DEFAULT_RESOURCE.getThreshold(), toCombine, MODULO);
        assertEquals(BigInteger.valueOf(42), res);
    }

    @Test
    void sunshineRandomizing() {
        Map<Integer, BigInteger> shares = DEFAULT_ENGINE.share(BigInteger.valueOf(42), MODULO);
        Map<Integer, BigInteger> randomZero = DEFAULT_ENGINE.randomPoly(DEFAULT_RESOURCE.getThreshold()*2, BigInteger.ZERO, MODULO);
        Map<Integer, BigInteger> toCombine = new HashMap<>();
        for (int i = 0; i < DEFAULT_RESOURCE.getParties(); i++) {
            toCombine.put(i, shares.get(i).add(randomZero.get(i)));
        }
        BigInteger res = DEFAULT_ENGINE.combine(2*(DEFAULT_RESOURCE.getThreshold()), toCombine, MODULO);
        assertEquals(BigInteger.valueOf(42), res);
    }
}
