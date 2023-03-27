package dk.jot2re.mult.paillier;

import java.math.BigInteger;
import java.security.SecureRandom;

public class Paillier {
    private final SecureRandom rand;

    public Paillier(SecureRandom rand) {
        this.rand = rand;
    }

    public PrivateKey generate(int bitlength) {
        BigInteger p = BigInteger.probablePrime(bitlength, rand);
        BigInteger q = BigInteger.probablePrime(bitlength, rand);
        BigInteger n = p.multiply(q);
        BigInteger lambda = p.subtract(BigInteger.ONE).multiply(q.subtract(BigInteger.ONE));
        if (!n.gcd(lambda).equals(BigInteger.ONE)) {
            return generate(bitlength);
        }
        BigInteger g = n.add(BigInteger.ONE);
        BigInteger mu = lambda.modInverse(n);
        PublicKey pk = new PublicKey(n, g);
        return new PrivateKey(pk, lambda, mu);
    }

//    private BigInteger L(BigInteger x, BigInteger n) {
//        return x.subtract(BigInteger.ONE).divide(n);
//    }
//
//    private BigInteger lcm(BigInteger a, BigInteger b) {
//        return a.multiply(b).abs().divide(a.gcd(b));
//    }

    public class PrivateKey {
        private final BigInteger lambda;
        private final BigInteger mu;
        private final PublicKey pk;

        public PrivateKey(PublicKey pk, BigInteger lambda, BigInteger mu) {
            this.lambda = lambda;
            this.mu = mu;
            this.pk = pk;
        }

        public BigInteger getLambda() {
            return lambda;
        }

        public BigInteger getMu() {
            return mu;
        }

        public PublicKey getPublicKey() {
            return pk;
        }
    }

    public class PublicKey {
        private final BigInteger n;
        private final BigInteger g;

        public PublicKey(BigInteger n, BigInteger g) {
            this.n = n;
            this.g = g;
        }

        public BigInteger getN() {
            return n;
        }

        public BigInteger getG() {
            return g;
        }
    }
}
