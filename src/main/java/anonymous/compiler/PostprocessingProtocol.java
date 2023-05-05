package anonymous.compiler;

import anonymous.mult.ot.commitment.HashBasedCommitment;
import anonymous.mult.ot.util.AesCtrDrbg;
import anonymous.mult.ot.util.Drbg;
import anonymous.network.INetwork;
import anonymous.rsa.our.OurParameters;
import anonymous.rsa.our.RSAUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static anonymous.mult.ot.util.Fiddling.ceil;

public class PostprocessingProtocol extends AbstractCompiledProtocol  {
    private static final Logger logger = LoggerFactory.getLogger(PostprocessingProtocol.class);
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
    private final OurParameters brainParam;
    private final OurParameters pinkyParam;
    private Drbg drbg;
    private SecureRandom commonRand;

    public PostprocessingProtocol(OurParameters brainParam, OurParameters pinkyParam, int compSec) {
        super(new CompiledProtocolResources(compSec));
        this.brainParam = brainParam;
        this.pinkyParam = pinkyParam;
    }

    @Override
    public void init(NetworkPair networks, Random random) {
        super.init(networks, random);
        brainParam.getMult().init(networks.getBrainNetwork(), random);
        pinkyParam.getMult().init(networks.getPinkyNetwork(), random);
        byte[] seed = new byte[ceil(resources.getCompSec(), 8)];
        random.nextBytes(seed);
        this.drbg = new AesCtrDrbg(seed);
    }

    public boolean execute(List<Multiplication> multiplications) {
        try {
            commonRand = SecureRandom.getInstance("SHA1PRNG", "SUN");
            commonRand.setSeed(coinToss(network));
        } catch (Exception e) {
            throw new RuntimeException("Could not set common randomness");
        }
        for (Multiplication cur: multiplications) {
            if (!validate(brainParam, cur) ){
                // TODO enable pinky when compiler works
//                || !validate(pinkyParam, cur)) {
                return false;
            }
        }
        return true;
    }

    private boolean validate(OurParameters params, Multiplication multiplication) {
        BigInteger modulo = multiplication.getModulo();
        BigInteger myAShare = RSAUtil.sample(random, modulo);
        Serializable aShare = params.getMult().shareFromAdditive(myAShare,modulo);
        Serializable cShare = params.getMult().multShares(aShare, multiplication.right, modulo);
        BigInteger r = new BigInteger(params.getStatBits(), commonRand);
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

    private byte[] coinToss(INetwork network) {
        try {
            // TODO can be optimized with hashing
            byte[] randomBytes = new byte[ceil(resources.getCompSec(), 8)];
            random.nextBytes(randomBytes);
            HashBasedCommitment com = new HashBasedCommitment();
            byte[] opening = com.commit(drbg, randomBytes);
            network.sendToAll(com.getCommitmentValue());
            Map<Integer, byte[]> otherCom = network.receiveFromAllPeers();
            for (int i : network.peers()) {
                broadcastValidation(network, i, network.peers(), otherCom.get(i));
            }
            network.sendToAll(opening);
            Map<Integer, byte[]> otherOpening = network.receiveFromAllPeers();
            byte[] res = randomBytes;
            for (int party: otherOpening.keySet()) {
                com.setCommitmentValue(otherCom.get(party));
                byte[] openedCom = com.open(otherOpening.get(party));
                for (int i = 0; i < randomBytes.length; i++) {
                    randomBytes[i] ^= openedCom[i];
                }
            }
            return res;
        } catch (Exception e) {
            throw new RuntimeException("Could not do coin tossing", e);
        }
    }
}
