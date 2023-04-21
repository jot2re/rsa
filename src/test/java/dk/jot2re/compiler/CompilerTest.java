package dk.jot2re.compiler;

import dk.jot2re.AbstractProtocolTest;

public class CompilerTest extends AbstractProtocolTest {
//    @ParameterizedTest
//    @CsvSource({"2,linear", "3,linear", "5,linear", "2,log", "3,log", "5,log", "2,const", "3,const", "5,const"})
//    public void sunshine(int parties, String type) throws Exception {
//        Map<Integer, BigInteger> pShares = RSATestUtils.randomPrime(parties, PRIME_BITLENGTH, rand);
//        Map<Integer, BigInteger> qShares = RSATestUtils.randomPrime(parties, PRIME_BITLENGTH, rand);
//        BigInteger p = pShares.values().stream().reduce(BigInteger.ZERO, (a, b) -> a.add(b));
//        BigInteger q = qShares.values().stream().reduce(BigInteger.ZERO, (a, b) -> a.add(b));
//        BigInteger N = p.multiply(q);
//
//        AbstractProtocolTest.RunProtocol<Boolean> protocolRunner = (param) -> {
//            OurParameters baseParam = (OurParameters) param;
//            OurProtocol.MembershipProtocol membership =  OurProtocol.MembershipProtocol.LINEAR;
//            OurProtocol brain = new OurProtocol(new OurParameters(baseParam.getPrimeBits(), baseParam.getStatBits(), baseParam.getP(), baseParam.getQ(), baseParam.getM(), ));
//            CompiledProtocol protocol = new CompiledProtocol(new OurProtocol((OurParameters) param, membership));
//            long start = System.currentTimeMillis();
//            boolean res = protocol.execute(pShares.get(param.getMyId()), qShares.get(param.getMyId()), N);
//            long stop = System.currentTimeMillis();
//            System.out.println("time: " + (stop-start));
//            return res;
//        };
//
//        AbstractProtocolTest.ResultCheck<Boolean> checker = (res) -> {
//            for (Future<Boolean> cur : res) {
//                assertTrue(cur.get());
//            }
//        };
//
//        Map<Integer, OurParameters> parameters = RSATestUtils.getOurParameters(PRIME_BITLENGTH, STAT_SEC, parties);
//        runProtocolTest(parameters, protocolRunner, checker);
//        System.out.println(((MultCounter) parameters.get(0).getMult()).toString());
//        System.out.println("Rounds " + ((DummyNetwork) parameters.get(0).getNetwork()).getRounds());
//        System.out.println("Nettime " + ((DummyNetwork) parameters.get(0).getNetwork()).getNetworkTime());
//        System.out.println("Nettrans " + ((DummyNetwork) parameters.get(0).getNetwork()).getTransfers());
//        System.out.println("Net bytes " + ((DummyNetwork) parameters.get(0).getNetwork()).getBytesSent());
//    }
}
