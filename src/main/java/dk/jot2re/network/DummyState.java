package dk.jot2re.network;

import java.io.Serializable;
import java.util.*;

public class DummyState {
    /**
     * Thread safe map, mapping from *recipient* to the deque of message pairs, of the sender and receiver
     */
    private final Map<Integer, Map<Integer, Deque<Serializable>>> messages;

    public DummyState(int parties) {
        messages = Collections.synchronizedMap(new HashMap<>(parties));
        for (int i = 0; i < parties; i++) {
            Map<Integer, Deque<Serializable>> curMap = Collections.synchronizedMap(new HashMap<>(parties));
            for (int j = 0; j < parties; j++) {
                curMap.put(j, new LinkedList<>());
            }
            messages.put(i, curMap);
        }
    }

    public synchronized void put(int senderId, int receiverId, Serializable message) throws IllegalAccessException {
        if (!messages.get(receiverId).get(senderId).add(message)) {
            throw new IllegalAccessException("Could not add data to network");
        }
    }

    public synchronized Serializable get(int senderId, int receiveId) {
        return messages.get(receiveId).get(senderId).pollFirst();
    }

    public int parties() {
        return messages.size();
    }
}
