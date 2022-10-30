package raft.core.rpc.exception;

public class ConnectorException extends RuntimeException {

    public ConnectorException(String message, Throwable cause) {
        super(message, cause);
    }

}
