package raft.core.log.execption;

/**
 * 日志复制统一异常处理
 * @author yiyewei
 * @create 2022/9/26 9:37
 **/
public class LogException extends RuntimeException {

    public LogException() {}

    public LogException(String message) {
        super(message);
    }

    public LogException(Throwable cause) {
        super(cause);
    }

    public LogException(String message, Throwable cause) {
        super(message, cause);
    }

}
