 package dk.jot2re.rsa;

import dk.jot2re.rsa.bf.BFParameters;
import dk.jot2re.rsa.our.RSAUtil;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static dk.jot2re.rsa.RSATestUtils.share;
import static org.junit.jupiter.api.Assertions.*;

public class RSAFiddlingTest {
    @ParameterizedTest
    @ValueSource(ints = {2, 3, 5})
    void testShare(int parties) {
        BigInteger modulo = BigInteger.TWO.pow(60);
        BigInteger ref = BigInteger.valueOf(1337);
        Map<Integer, BigInteger> shares = share(ref, parties, modulo, new Random(42));
        assertEquals(ref, shares.values().stream().reduce(BigInteger.ZERO, (a, b) -> a.add(b).mod(modulo)));
        assertEquals(shares.size(), parties);
    }

    @ParameterizedTest
    @CsvSource({"2,2", "2,5", "3,2", "3,13", "5,8"})
    public void testMultList(int parties, int amount) throws Exception {
        int bits = 32;
        Map<Integer, BFParameters> params = RSATestUtils.getBFParameters(bits, 20, parties);
        BigInteger modulo = BigInteger.probablePrime(bits, params.get(0).getRandom());
        BigInteger[] ref = new BigInteger[amount];
        for (int j = 0; j < amount; j++) {
            ref[j] = BigInteger.ZERO;
        }
        Map<Integer, BigInteger[]> toMult = new HashMap<>(parties);
        for (int i = 0; i < parties; i++) {
            BigInteger[] cur = new BigInteger[amount];
            for (int j = 0; j < amount; j++) {
                cur[j] = new BigInteger(bits, params.get(i).getRandom());
                ref[j] = ref[j].add(cur[j]).mod(modulo);
            }
            toMult.put(i, cur);
        }

        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        List<Future<BigInteger>> shares = new ArrayList<>(parties);
        for (int i = 0; i < parties; i++) {
            int finalI = i;
            shares.add(executor.submit(() -> params.get(finalI).getMult().combineToAdditive(RSAUtil.multList(params.get(finalI), toMult.get(finalI), modulo), modulo)));
        }
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));

        BigInteger expected = Arrays.stream(ref).reduce(BigInteger.ONE, (a, b)-> a.multiply(b).mod(modulo));
        BigInteger computed = BigInteger.ZERO;
        for (int i = 0; i < shares.size(); i++) {
            computed = computed.add(shares.get(i).get()).mod(modulo);
        }
        assertEquals(expected, computed);
        for (int i = 0; i < shares.size(); i++) {
            assertNotEquals(BigInteger.ZERO, shares.get(i).get());
            assertNotEquals(BigInteger.ONE, shares.get(i).get());
        }
    }
}
