package dk.jot2re.rsa.our;

import dk.jot2re.rsa.bf.BFParameters;

import java.math.BigInteger;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class RSAUtil {
    public static List<BigInteger> sample(BFParameters params, BigInteger modulo, int amount) {
        return IntStream.range(0, amount).mapToObj(i -> sample(params, modulo)).collect(Collectors.toList());
    }

    public static BigInteger sample(BFParameters params, BigInteger modulo) {
        BigInteger r = new BigInteger(modulo.bitLength()+params.getStatBits(), params.getRandom());
        return r.mod(modulo);
    }

    public static BigInteger multList(BFParameters params, List<BigInteger> shares) {
        BigInteger temp = shares.get(0);
        for (int i = 1; i < shares.size(); i++) {
            temp = params.getMult().mult(temp, shares.get(i));
        }
        return temp;
    }


    public static BigInteger multListConstRounds(BFParameters params, List<BigInteger> shareA, List<BigInteger> shareB) {
        // TODO make low-round optimization
        return null;
    }
}
