package raft.store.excpetion;

public class writeException extends RuntimeException{
    public writeException(String message) {
        super(message);
    }
}
