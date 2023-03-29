package dk.jot2re.mult.gilboa.oracle;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

public class PrgTest {
    private static Random random;

    @BeforeAll
    static void setup() {
        random = new Random(42);
    }

    @Test
    void sunshine() {
        Prg prg = new Prg(new byte[32]);
        byte[] bytes = prg.getBytes(1);
        assertArrayEquals(new byte[] {(byte) 0x85}, bytes);
        byte[] moreBytes = prg.getBytes(1);
        assertArrayEquals(new byte[] {(byte) 0x2A}, moreBytes);
    }

    @Test
    void determinism() {
        byte[] seed = new byte[32];
        random.nextBytes(seed);
        Prg prg1 = new Prg(seed);
        byte[] res1 = prg1.getBytes(10);
        Prg prg2 = new Prg(seed);
        byte[] res2 = prg2.getBytes(10);
        assertArrayEquals(res1, res2);
        byte[] res3 = prg2.getBytes(10);
        assertFalse(Arrays.equals(res2, res3));
    }

    @ParameterizedTest
    @CsvSource({"1,33", "64,11","32,65"})
    void differentParameters(int seedLength, int amount) {
        byte[] seed = new byte[seedLength];
        random.nextBytes(seed);
        Prg prg = new Prg(new byte[32]);
        byte[] bytes = prg.getBytes(amount);
        assertEquals(amount, bytes.length);
        assertFalse(Arrays.equals(new byte[amount], bytes));
        byte[] moreBytes = prg.getBytes(amount);
        assertEquals(amount, moreBytes.length);
        assertFalse(Arrays.equals(new byte[amount], moreBytes));
        assertFalse(Arrays.equals(bytes, moreBytes));
    }

    @Test
    void badAmount() {
        Exception thrown = Assertions.assertThrows(Exception.class, () -> {
            Prg prg = new Prg(new byte[32]);
            prg.getBytes(-1);
        });
        Assertions.assertEquals("Amount of bytes must be positive", thrown.getMessage());
    }
}
