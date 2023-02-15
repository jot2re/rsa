import dk.jot2re.network.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigInteger;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DummyNetworkTest {

    @ParameterizedTest
    @ValueSource(ints = { 2, 3, 5})
    public void sunshine(int parties) throws Exception {
        BigInteger message = new BigInteger("4687860186094695676870");

        DummyNetworkFactory factory = new DummyNetworkFactory(parties);
        Map<Integer, INetwork> networks = factory.getNetworks();
        for (int i = 0; i < parties; i++) {
            for (int j = 0; j < parties; j++) {
                networks.get(i).send(j, message.add(BigInteger.valueOf(i)));
                networks.get(i).send(j, BigInteger.valueOf(42));
            }
        }
        for (int i = 0; i < parties; i++) {
            for (int j = 0; j < parties; j++) {
                BigInteger msg1 = (BigInteger) networks.get(i).receive(j);
                assertEquals(message.add(BigInteger.valueOf(i)), msg1);
                BigInteger msg2 = (BigInteger) networks.get(i).receive(j);
                assertEquals(BigInteger.valueOf(42), msg2);
            }
        }
    }

    @ParameterizedTest
    @ValueSource(ints = { 2, 3, 5})
    public void sendToAll(int parties) throws Exception {
        BigInteger message = new BigInteger("4687860186094695676870");

        DummyNetworkFactory factory = new DummyNetworkFactory(parties);
        Map<Integer, INetwork> networks = factory.getNetworks();
        for (int i = 0; i < parties; i++) {
            networks.get(i).sendToAll(message.add(BigInteger.valueOf(i)));
            networks.get(i).sendToAll(BigInteger.valueOf(42));
        }
        for (int i = 0; i < parties; i++) {
            BigInteger msg1 = (BigInteger) networks.get(i).receive(i);
            assertEquals(message.add(BigInteger.valueOf(i)), msg1);
            BigInteger msg2 = (BigInteger) networks.get(i).receive(i);
            assertEquals(BigInteger.valueOf(42), msg2);
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
        assertEquals(3, network.peers());
    }
}
