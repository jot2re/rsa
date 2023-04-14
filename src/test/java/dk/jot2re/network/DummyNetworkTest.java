package dk.jot2re.network;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigInteger;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DummyNetworkTest {

    @ParameterizedTest
    @ValueSource(ints = { 2, 3, 5})
    public void sunshine(int parties) throws Exception {
        BigInteger message = new BigInteger("4687860186094695676870");

        NetworkFactory factory = new NetworkFactory(parties);
        Map<Integer, DummyNetwork> networks = factory.getNetworks(NetworkFactory.NetworkType.DUMMY);
        for (int i = 0; i < parties; i++) {
            for (int j : networks.get(i).peers()) {
                networks.get(i).send(j, message.add(BigInteger.valueOf(i)));
                networks.get(i).send(j, BigInteger.valueOf(42));
            }
        }
        for (int i = 0; i < parties; i++) {
            for (int j : networks.get(i).peers()) {
                BigInteger msg1 = (BigInteger) networks.get(i).receive(j);
                assertEquals(message.add(BigInteger.valueOf(j)), msg1);
                BigInteger msg2 = (BigInteger) networks.get(i).receive(j);
                assertEquals(BigInteger.valueOf(42), msg2);
            }
        }
    }

    @ParameterizedTest
    @ValueSource(ints = { 2, 3, 5})
    public void sendToAll(int parties) throws Exception {
        BigInteger message = new BigInteger("4687860186094695676870");

        NetworkFactory factory = new NetworkFactory(parties);
        Map<Integer, DummyNetwork> networks = factory.getNetworks(NetworkFactory.NetworkType.DUMMY);
        for (int i = 0; i < parties; i++) {
            networks.get(i).sendToAll(message.add(BigInteger.valueOf(i)));
            networks.get(i).sendToAll(BigInteger.valueOf(42));
        }
        for (int i = 0; i < parties; i++) {
            for (int j : networks.get(i).peers()) {
                BigInteger msg1 = (BigInteger) networks.get(i).receive(j);
                assertEquals(message.add(BigInteger.valueOf(j)), msg1);
                BigInteger msg2 = (BigInteger) networks.get(i).receive(j);
                assertEquals(BigInteger.valueOf(42), msg2);
            }
        }
    }

    @Test
    public void p2pGetters() {
        DummyP2P p2p = new DummyP2P(null, 1, 2);
        assertEquals(1, p2p.myId());
        assertEquals(2, p2p.peerId());
    }

    @Test
    public void networkGetters() {
        DummyNetwork network = new DummyNetwork(new DummyState(3), 1);
        assertEquals(1, network.myId());
        assertEquals(2, network.peers().size());
    }

    @Test
    public void dataCount() throws Exception {
        int parties = 3;
        NetworkFactory factory = new NetworkFactory(parties);
        Map<Integer, DummyNetwork> networks = factory.getNetworks(NetworkFactory.NetworkType.DUMMY);
        for (int i = 1; i < parties; i++) {
            networks.get(i).send(0, BigInteger.valueOf(1337));
            networks.get(i).send(0, BigInteger.valueOf(42));
            assertEquals(2, networks.get(i).getTransfers());
            assertEquals(0, networks.get(i).getRounds());
            assertTrue(networks.get(i).getBytesSent() > 2*64);
            // The overhead of big int is about 200 bytes
            assertTrue(networks.get(i).getBytesSent() < 2*210);
        }
    }

    @Test
    public void roundCount() throws Exception {
        int parties = 3;
        NetworkFactory factory = new NetworkFactory(parties);
        Map<Integer, DummyNetwork> networks = factory.getNetworks(NetworkFactory.NetworkType.DUMMY);
        for (int i = 1; i < parties; i++) {
            networks.get(i).send(0, BigInteger.valueOf(1337));
            networks.get(i).send(0, BigInteger.valueOf(42));
            assertEquals(2, networks.get(i).getTransfers());
            assertEquals(0, networks.get(i).getRounds());
            assertTrue(networks.get(i).getBytesSent() > 2*64);
            // The overhead of big int is about 200 bytes
            assertTrue(networks.get(i).getBytesSent() < 2*210);
        }
        for (int i = 1; i < parties; i++) {
            BigInteger val1 = (BigInteger) networks.get(0).receive(i);
            assertEquals(BigInteger.valueOf(1337), val1);
            BigInteger val2 = (BigInteger) networks.get(0).receive(i);
            assertEquals(BigInteger.valueOf(42), val2);
        }
        assertEquals(1, networks.get(0).getRounds());
    }

    @Test
    public void roundCount2() throws Exception {
        int parties = 2;
        NetworkFactory factory = new NetworkFactory(parties);
        Map<Integer, DummyNetwork> networks = factory.getNetworks(NetworkFactory.NetworkType.DUMMY);
        networks.get(0).send(1, BigInteger.valueOf(1337));
        networks.get(1).send(0, BigInteger.valueOf(42));
        BigInteger val1 = (BigInteger) networks.get(0).receive(1);
        assertEquals(BigInteger.valueOf(42), val1);
        BigInteger val2 = (BigInteger) networks.get(1).receive(0);
        assertEquals(BigInteger.valueOf(1337), val2);
        networks.get(0).send(1, BigInteger.valueOf(42));
        networks.get(1).send(0, BigInteger.valueOf(1337));
        BigInteger val3 = (BigInteger) networks.get(0).receive(1);
        assertEquals(BigInteger.valueOf(1337), val3);
        BigInteger val4 = (BigInteger) networks.get(1).receive(0);
        assertEquals(BigInteger.valueOf(42), val4);
        assertEquals(2, networks.get(0).getRounds());
        assertEquals(2, networks.get(1).getRounds());
//        // No new message to receive
//        networks.get(0).receive(1);
//        assertEquals(2, networks.get(0).getRounds());
        // We don't count a round until the receive phase
        networks.get(0).send(1, BigInteger.valueOf(1337));
        assertEquals(2, networks.get(0).getRounds());
    }
}
