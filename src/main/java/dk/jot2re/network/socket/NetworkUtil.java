package dk.jot2re.network.socket;

import dk.jot2re.mult.ot.util.ExceptionConverter;
import dk.jot2re.network.INetwork;

import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class NetworkUtil {
    /**
     * Creates a map of party IDs to network address (uses localhost and supplied ports) information.
     *
     * @return a map of party IDs to network address information
     */
    public static Map<Integer, NetworkConfiguration> getNetworkConfigurations(List<Integer> ports) {
        Map<Integer, NetworkConfiguration> confs = new HashMap<>(ports.size());
        Map<Integer, Party> partyMap = new HashMap<>();
        int id = 0;
        for (int port : ports) {
            partyMap.put(id, new Party(id, "localhost", port));
            id++;
        }
        for (int i = 0; i < ports.size(); i++) {
            confs.put(i, new NetworkConfigurationImpl(i, partyMap));
        }
        return confs;
    }

    /**
     * Finds {@code portsRequired} free ports and returns their port numbers. <p>NOTE: two subsequent
     * calls to this method can return overlapping sets of free ports (same with parallel calls).</p>
     *
     * @param portsRequired number of free ports required
     * @return list of port numbers of free ports
     */
    public static List<Integer> getFreePorts(int portsRequired) {
        List<ServerSocket> sockets = new ArrayList<>(portsRequired);
        for (int i = 0; i < portsRequired; i++) {
            ServerSocket s = ExceptionConverter.safe(() -> new ServerSocket(0),
                    "Could not create new server socket.");
            sockets.add(s);
            // we keep the socket open to ensure that the port is not re-used in a sub-sequent iteration
        }
        return sockets.stream().map(socket -> {
            int portNumber = socket.getLocalPort();
            ExceptionConverter.safe(() -> {
                socket.close();
                return null;
            }, "Could not close server port");
            return portNumber;
        }).collect(Collectors.toList());
    }

    /**
     * As getConfigurations(n, ports) but tries to find free ephemeral ports (but note that there is
     * no guarantee that ports will remain unused).
     */
    public static Map<Integer, INetwork> getNetworkConfigurations(int n) throws Exception {
        List<Integer> ports = getFreePorts(n);
        Map<Integer, NetworkConfiguration> configurationMap =  getNetworkConfigurations(ports);
        Map<Integer,Future<INetwork>> futureNetworks = new HashMap<>(n);
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        for (int partyId: configurationMap.keySet()) {
            Future<INetwork> curMult = executor.submit(() -> {
                return new SocketNetwork(configurationMap.get(partyId));
            });
            futureNetworks.put(partyId, curMult);
        }
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
        Map<Integer, INetwork> res = new HashMap<>(n);
        for (int i: futureNetworks.keySet()) {
            res.put(i, futureNetworks.get(i).get());
        }
        return res;
    }

}
