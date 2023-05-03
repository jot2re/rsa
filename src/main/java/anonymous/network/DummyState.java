package anonymous.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.*;

public class DummyState {
    private static final Logger logger = LoggerFactory.getLogger(DummyState.class);
    /**
     * Thread safe map, mapping from *recipient* to the deque of message pairs, of the sender and receiver
     */
    private final Map<Integer, Map<Integer, Queue<Serializable>>> messages;

    public DummyState(int parties) {
        messages = Collections.synchronizedMap(new HashMap<>(parties));
        for (int i = 0; i < parties; i++) {
            Map<Integer, Queue<Serializable>> curMap = Collections.synchronizedMap(new HashMap<>(parties));
            for (int j = 0; j < parties; j++) {
                curMap.put(j, new LinkedList<>());
            }
            messages.put(i, curMap);
        }
    }

    public synchronized void put(int senderId, int receiverId, Serializable message) throws IllegalAccessException {
        if (!messages.get(receiverId).get(senderId).add(message)) {
            logger.error("Could not add data");
            throw new IllegalAccessException("Could not add data to network");
        }
    }

    public synchronized Serializable get(int senderId, int receiveId) {
        return messages.get(receiveId).get(senderId).poll();
    }

    public int parties() {
        return messages.size();
    }
}
