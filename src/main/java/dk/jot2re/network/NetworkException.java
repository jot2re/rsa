package dk.jot2re.network;

public class NetworkException extends Exception {
    public NetworkException(String errorMessage, Throwable err) {
        super(errorMessage, err);
    }
}
