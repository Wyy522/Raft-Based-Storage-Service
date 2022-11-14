package raft.store.excpetion;

public class ioException extends RuntimeException{
    public ioException(String message) {
        super(message);
    }
}
