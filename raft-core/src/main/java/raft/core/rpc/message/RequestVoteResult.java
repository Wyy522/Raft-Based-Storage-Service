package raft.core.rpc.message;

/**
 * 投票请求的响应类(一般由follower发起)
 * @author yiyewei
 * @create 2022/9/20 15:27
 **/
public class RequestVoteResult {

    //自己当前的任期号
    private final int term;

    //是否投票给这个candidate
    private final boolean voteGranted;

    public RequestVoteResult(int term, boolean voteGranted) {
        this.term = term;
        this.voteGranted = voteGranted;
    }

    public int getTerm() {
        return term;
    }

    public boolean isVoteGranted() {
        return voteGranted;
    }

    @Override
    public String toString() {
        return "RequestVoteResult{" +
                "term=" + term +
                ", voteGranted=" + voteGranted +
                '}';
    }
}
