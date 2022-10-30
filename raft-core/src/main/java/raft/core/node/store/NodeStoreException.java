package raft.core.node.store;

/**
 * 无法将存储状态落盘时抛出
 * @author yiyewei
 * @create 2022/9/20 23:50
 **/
public class NodeStoreException extends RuntimeException {


    public NodeStoreException(Throwable cause) {
        super(cause);
    }


    public NodeStoreException(String message, Throwable cause) {
        super(message, cause);
    }

}
