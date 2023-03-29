package dk.jot2re.mult.gilboa.oracle;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

public class RandomOracleTest {
    private static Random random;

    @BeforeAll
    static void setup() {
        random = new Random(42);
    }


    @Test
    void sunshine() {
        byte[] emptySeed = new byte[32];
        RandomOracle oracle = new RandomOracle(emptySeed, 2);
        byte[] res = oracle.apply(new byte[1]);
        byte[] ref = new byte[] {(byte) 0xFE, (byte) 0xCC};
        assertArrayEquals(ref, res);
    }

    @ParameterizedTest
    @CsvSource({"1,33,1", "64,11,35","32,65,64"})
    void differentParameters(int seedLength, int imageInBytes, int inputLength) {
        byte[] seed = new byte[seedLength];
        random.nextBytes(seed);
        RandomOracle oracle = new RandomOracle(seed, imageInBytes);
        byte[] input = new byte[inputLength];
        random.nextBytes(input);
        byte[] res = oracle.apply(input);
        assertEquals(imageInBytes, res.length);
        assertFalse(Arrays.equals(new byte[imageInBytes], res));
    }

    @Test
    void nullSeed() {
        Exception thrown = Assertions.assertThrows(Exception.class, () -> {
            new RandomOracle(null, 32);
        });
        Assertions.assertEquals("Invalid input", thrown.getMessage());
    }

    @Test
    void badInputSize() {
        Exception thrown = Assertions.assertThrows(Exception.class, () -> {
            new RandomOracle(new byte[32], 0);
        });
        Assertions.assertEquals("Invalid input", thrown.getMessage());
    }

    @Test
    void badApplyInput() {
        Exception thrown = Assertions.assertThrows(Exception.class, () -> {
            RandomOracle oracle = new RandomOracle(new byte[32], 32);
            oracle.apply(null);
        });
        Assertions.assertEquals("Empty input", thrown.getMessage());
    }

    @Test
    void badApplyInput2() {
        Exception thrown = Assertions.assertThrows(Exception.class, () -> {
            RandomOracle oracle = new RandomOracle(new byte[32], 32);
            oracle.apply(new byte[0]);
        });
        Assertions.assertEquals("Empty input", thrown.getMessage());
    }
}
