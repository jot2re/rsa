package anonymous.compiler;

import anonymous.AbstractProtocol;
import anonymous.mult.ot.commitment.HashBasedCommitment;
import anonymous.mult.ot.util.AesCtrDrbg;
import anonymous.mult.ot.util.Drbg;
import anonymous.network.INetwork;
import anonymous.rsa.our.OurParameters;
import anonymous.rsa.our.RSAUtil;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Postprocessing extends AbstractProtocol  {
    public static class Multiplication {
        private final Serializable left;
        private final Serializable right;
        private final Serializable output;
        private final BigInteger modulo;

        public Multiplication(Serializable left, Serializable right, Serializable output, BigInteger modulo) {
            this.left = left;
            this.right = right;
            this.output = output;
            this.modulo = modulo;
        }

        public Serializable getLeft() {
            return left;
        }

        public Serializable getRight() {
            return right;
        }

        public Serializable getOutput() {
            return output;
        }

        public BigInteger getModulo() {
            return modulo;
        }
    }
    private final OurParameters params;
    private HashBasedCommitment com;
    private Drbg drbg;

    public Postprocessing(OurParameters params) {
        this.params = params;
    }

    @Override
    public void init(INetwork network, Random random) {
        super.init(network, random);
        params.getMult().init(network, random);
        this.com = new HashBasedCommitment();
        // TODO should not be hardcoded
        byte[] seed = new byte[32];
        random.nextBytes(seed);
        this.drbg = new AesCtrDrbg(seed);
    }

    public boolean execute(List<Multiplication> multiplications) {
        for (Multiplication cur: multiplications) {
            if (!validate(cur)) {
                return false;
            }
        }
        return true;
    }

    private boolean validate(Multiplication multiplication) {
        BigInteger modulo = multiplication.getModulo();
        BigInteger myAShare = RSAUtil.sample(random, modulo);
        Serializable aShare = params.getMult().shareFromAdditive(myAShare,modulo);
        Serializable cShare = params.getMult().multShares(aShare, multiplication.right, modulo);
        BigInteger r = coinToss();
        Serializable eShare = params.getMult().add(params.getMult().multConst(multiplication.left, r, modulo), aShare, modulo);
        BigInteger e = params.getMult().open(eShare, modulo);
        Serializable zeroCand = params.getMult().sub(
                params.getMult().add(
                    params.getMult().multConst(multiplication.output, r, modulo),
                    cShare, modulo),
                params.getMult().multConst(multiplication.right, e, modulo), modulo);
        BigInteger zero = params.getMult().open(zeroCand, modulo);
        return zero.equals(BigInteger.ZERO);
    }

    private BigInteger coinToss() {
        try {
            // TODO should use broadcast!
            // TODO can be optimized with hashing
            BigInteger myShare = new BigInteger(params.getStatBits(), random);
            byte[] opening = com.commit(drbg, myShare.toByteArray());
            network.sendToAll(com.getCommitmentValue());
            Map<Integer, byte[]> otherCom = network.receiveFromAllPeers();
            network.sendToAll(opening);
            Map<Integer, byte[]> otherOpening = network.receiveFromAllPeers();
            BigInteger random = myShare;
            for (int party: otherOpening.keySet()) {
                com.setCommitmentValue(otherCom.get(party));
                byte[] openedCom = com.open(otherOpening.get(party));
                random = random.add(new BigInteger(1, openedCom));
            }
            return random.mod(BigInteger.TWO.pow(params.getStatBits()));
        } catch (Exception e) {
            throw new RuntimeException("Could not do coin tossing");
        }
    }
}
