package anonymous.rsa.our;

import anonymous.network.INetwork;
import anonymous.network.NetworkException;
import anonymous.rsa.bf.BFParameters;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static anonymous.DefaultSecParameters.REJECTION_SAMPLING;
import static anonymous.DefaultSecParameters.STAT_SEC;

public class RSAUtil {
    // TODO move to mult func

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

    public static BigInteger open(INetwork network, BigInteger share, BigInteger modulo) throws NetworkException {
        network.sendToAll(share);
        Map<Integer, BigInteger> otherShares = network.receiveFromAllPeers();
        return otherShares.values().stream().reduce(share, (a, b) -> a.add(b)).mod(modulo);
    }

    public static BigInteger addConst(int myId, BigInteger share, BigInteger constant, BigInteger modulo) throws NetworkException {
        if (myId == 0) {
            return share.add(constant);
        } else {
            return share;
        }
    }

    public static BigInteger subConst(int myId, BigInteger share, BigInteger constant, BigInteger modulo) throws NetworkException {
        if (myId == 0) {
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
