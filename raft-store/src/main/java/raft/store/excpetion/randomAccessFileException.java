package raft.store.excpetion;

public class randomAccessFileException extends RuntimeException {
    public randomAccessFileException() {
    }

    public randomAccessFileException(String message) {
        super(message);
    }

    public randomAccessFileException(String message, Throwable cause) {
        super(message, cause);
    }

    public randomAccessFileException(Throwable cause) {
        super(cause);
    }

    public randomAccessFileException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
