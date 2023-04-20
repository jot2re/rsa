package dk.jot2re.rsa.our;

import dk.jot2re.network.NetworkException;
import dk.jot2re.rsa.Parameters;
import dk.jot2re.rsa.bf.BFParameters;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static dk.jot2re.DefaultSecParameters.REJECTION_SAMPLING;
import static dk.jot2re.DefaultSecParameters.STAT_SEC;

public class RSAUtil {
    // TODO move to mult func
    public static List<BigInteger> share(Parameters params, BigInteger value, BigInteger modulo) {
        List<BigInteger> randomVals = IntStream.range(0, params.getAmountOfPeers()).mapToObj(i -> sample(params.getRandom(), modulo)).collect(Collectors.toList());
        randomVals.add(value.subtract(randomVals.stream().reduce(BigInteger.ZERO, BigInteger::add)).mod(modulo));
        return randomVals;
    }

    public static List<BigInteger> randomSharing(Parameters params, BigInteger modulo) {
        return IntStream.range(0, params.getAmountOfPeers()+1).mapToObj(i -> sample(params.getRandom(), modulo)).collect(Collectors.toList());
    }

    public static BigInteger sample(Random random, BigInteger modulo) {
        if (REJECTION_SAMPLING) {
            BigInteger share = new BigInteger(modulo.bitLength(), random);
            while (share.compareTo(modulo) >= 0) {
                share = new BigInteger(modulo.bitLength(), random);
            }
            return share;
        } else {
            BigInteger r = new BigInteger(modulo.bitLength()+STAT_SEC, random);
            return r.mod(modulo);
        }
    }

    public static BigInteger open(Parameters params, BigInteger share, BigInteger modulo) throws NetworkException {
        params.getNetwork().sendToAll(share);
        Map<Integer, BigInteger> otherShares = params.getNetwork().receiveFromAllPeers();
        return otherShares.values().stream().reduce(share, (a, b) -> a.add(b)).mod(modulo);
    }

    public static BigInteger addConst(Parameters params, BigInteger share, BigInteger constant, BigInteger modulo) throws NetworkException {
        if (params.getMyId() == 0) {
            return share.add(constant);
        } else {
            return share;
        }
    }

    public static BigInteger subConst(Parameters params, BigInteger share, BigInteger constant, BigInteger modulo) throws NetworkException {
        if (params.getMyId() == 0) {
            return share.subtract(constant);
        } else {
            return share;
        }
    }

    public static Serializable multList(BFParameters params, Serializable[] shares, BigInteger modulo) {
        if (shares.length < 2) {
            throw new IllegalArgumentException("Empty or singleton list");
        }
        Serializable temp = params.getMult().multShares(shares[0], shares[1], modulo);
        for (int i = 2; i < shares.length; i++) {
            temp = params.getMult().multShares(temp, shares[i], modulo);
        }
        return temp;
    }


    public static BigInteger multListConstRounds(BFParameters params, List<BigInteger> shareA, List<BigInteger> shareB) {
        // TODO make low-round optimization
        return null;
    }
}
