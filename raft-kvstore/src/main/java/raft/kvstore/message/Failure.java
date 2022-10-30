package raft.kvstore.message;

/**
 * 异常情况
 * @author yiyewei
 * @create 2022/10/9 9:50
 **/
public class Failure {
    private final int errorCode;
    private final String message;

    public Failure(int errorCode, String message) {
        this.errorCode = errorCode;
        this.message = message;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "Failure{" +
                "errorCode=" + errorCode +
                ", message=" + message +
                '}';
    }
}
