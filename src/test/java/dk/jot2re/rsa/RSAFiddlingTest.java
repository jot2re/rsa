 package dk.jot2re.rsa;

 import dk.jot2re.mult.IShare;
 import dk.jot2re.mult.IntegerShare;
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
 import static org.junit.jupiter.api.Assertions.assertEquals;
 import static org.junit.jupiter.api.Assertions.assertTrue;

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
        Map<Integer, IShare[]> toMult = new HashMap<>(parties);
        for (int i = 0; i < parties; i++) {
            IShare[] cur = new IShare[amount];
            for (int j = 0; j < amount; j++) {
                BigInteger curRand = new BigInteger(bits, params.get(i).getRandom());
                cur[j] = new IntegerShare(curRand, modulo);
                ref[j] = ref[j].add(curRand).mod(modulo);
            }
            toMult.put(i, cur);
        }

        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        List<Future<BigInteger>> shares = new ArrayList<>(parties);
        for (int i = 0; i < parties; i++) {
            int finalI = i;
            shares.add(executor.submit(() -> {
                IShare res = RSAUtil.multList(params.get(finalI),toMult.get(finalI), modulo);
                return params.get(finalI).getMult().open(res, modulo);
            }));
        }
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));

        BigInteger expected = Arrays.stream(ref).reduce(BigInteger.ONE, (a, b)-> a.multiply(b).mod(modulo));
        for (int i = 0; i < shares.size(); i++) {
            assertEquals(expected, shares.get(i).get());
        }
    }
}
