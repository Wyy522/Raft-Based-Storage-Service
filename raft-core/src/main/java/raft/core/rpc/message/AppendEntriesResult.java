package raft.core.rpc.message;


/**
 * 追加日志请求的响应Rpc类(由follower发起)
 * @author yiyewei
 * @create 2022/9/20 15:33
 **/

public class AppendEntriesResult {


    private final String rpcMessageId;
    //自己当前的任期号
    private final int term;

    //是否追加日志成功
    private final boolean success;

    public AppendEntriesResult(String rpcMessageId, int term, boolean success) {
        this.rpcMessageId = rpcMessageId;
        this.term = term;
        this.success = success;
    }

    public int getTerm() {
        return term;
    }

    public boolean isSuccess() {
        return success;
    }

    @Override
    public String toString() {
        return "AppendEntriesRpc{" +
                "term=" + term +
                ", success=" + success +
                '}';
    }

    public String getRpcMessageId() {
        return rpcMessageId;
    }
}
