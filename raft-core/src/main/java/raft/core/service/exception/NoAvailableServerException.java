package raft.core.service.exception;

public class NoAvailableServerException extends RuntimeException {
    public NoAvailableServerException(String message) {
        super(message);
    }

}
