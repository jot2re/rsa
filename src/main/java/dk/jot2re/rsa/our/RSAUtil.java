package dk.jot2re.rsa.our;

import dk.jot2re.network.NetworkException;
import dk.jot2re.rsa.Parameters;
import dk.jot2re.rsa.bf.BFParameters;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class RSAUtil {
    public static List<BigInteger> sample(Parameters params, BigInteger modulo, int amount) {
        return IntStream.range(0, amount).mapToObj(i -> sample(params, modulo)).collect(Collectors.toList());
    }

    public static BigInteger sample(Parameters params, BigInteger modulo) {
        BigInteger r = new BigInteger(modulo.bitLength()+params.getStatBits(), params.getRandom());
        return r.mod(modulo);
    }

    public static BigInteger open(Parameters params, BigInteger share, BigInteger modulo) throws NetworkException {
        params.getNetwork().sendToAll(share);
        Map<Integer, BigInteger> otherShares = params.getNetwork().receiveFromAllPeers();
        return otherShares.values().stream().reduce(share, (a, b) -> a.add(b).mod(modulo));
    }

    public static BigInteger addConst(Parameters params, BigInteger share, BigInteger constant, BigInteger modulo) throws NetworkException {
        if (params.getMyId() == 0) {
            return share.add(constant).mod(modulo);
        } else {
            return share;
        }
    }

    public static BigInteger subConst(Parameters params, BigInteger share, BigInteger constant, BigInteger modulo) throws NetworkException {
        if (params.getMyId() == 0) {
            return share.subtract(constant).mod(modulo);
        } else {
            return share;
        }
    }

    public static BigInteger multList(BFParameters params, BigInteger[] shares, BigInteger modulo) {
        if (shares.length < 2) {
            throw new IllegalArgumentException("Empty or singleton list");
        }
        BigInteger temp = params.getMult().mult(shares[0], shares[1], modulo);
        for (int i = 2; i < shares.length; i++) {
            temp = params.getMult().mult(temp, shares[i], modulo);
        }
        return temp;
    }


    public static BigInteger multListConstRounds(BFParameters params, List<BigInteger> shareA, List<BigInteger> shareB) {
        // TODO make low-round optimization
        return null;
    }
}
