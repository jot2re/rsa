package dk.jot2re.rsa.bf;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ProtocolTest {
    // First 3 tests are from https://www.mathworks.com/help/symbolic/jacobisymbol.html
    @ParameterizedTest
    @CsvSource({"1,9,1", "28,9,1", "14,561,1", "1353,566480805,0", "1353,566480807,1","7,23,-1"})
    public void jacobiTest(int a, int n, int res) {
        assertEquals(res, Protocol.jacobiSymbol(BigInteger.valueOf(a), BigInteger.valueOf(n)));
    }
}
