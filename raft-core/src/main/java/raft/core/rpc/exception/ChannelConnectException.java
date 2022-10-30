package raft.core.rpc.exception;

public class ChannelConnectException extends ChannelException {

    public ChannelConnectException(Throwable cause) {
        super(cause);
    }

    public ChannelConnectException(String message, Throwable cause) {
        super(message, cause);
    }

}
