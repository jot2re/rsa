package dk.jot2re.network;

import dk.jot2re.mult.ot.util.StrictBitVector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;

public class Serializer {
    private static final Logger logger = LoggerFactory.getLogger(Serializer.class);
    private final ByteArrayOutputStream byteStream;
    private final ObjectOutputStream writer;

    public Serializer() {
        try {
            byteStream = new ByteArrayOutputStream();
            writer = new ObjectOutputStream(byteStream);
        } catch (Exception e) {
            throw new RuntimeException("Could not initialize p2p network", e);
        }
    }

    public byte[] serialize(Serializable data) {
        byte[] res = null;
        try {
            if (data instanceof BigInteger) {
                writer.write(((BigInteger) data).toByteArray());
            } else if (data instanceof StrictBitVector) {
                writer.write(((StrictBitVector) data).toByteArray());
            } else if (data instanceof byte[]) {
                writer.write((byte[]) data);
            } else if (data instanceof ArrayList<?>) {
                for (int i = 0; i < ((ArrayList<?>) data).size(); i++) {
                    if (((ArrayList<?>) data).get(i) instanceof BigInteger) {
                        writer.write(((BigInteger) ((ArrayList<?>) data).get(i)).toByteArray());
                    } else if (((ArrayList<?>) data).get(i) instanceof byte[]) {
                        writer.write((byte[]) ((ArrayList<?>) data).get(i));
                    } else {
                        throw new RuntimeException("unknown array type");
                    }
                }
            } else {
                logger.warn("serializing unknown class " + data.getClass().descriptorString());
                writer.writeObject(data);
            }
            writer.flush();
            res = byteStream.toByteArray();
            byteStream.reset();
            writer.reset();
        } catch (Exception e) {
            logger.error("ERROR: " +  e.getMessage());
            throw new RuntimeException(e.getMessage(), e);
        }
        return res;
    }
}
