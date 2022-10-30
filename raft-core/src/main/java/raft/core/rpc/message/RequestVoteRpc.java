package raft.core.rpc.message;

import raft.core.node.base.NodeId;

/**
 * 投票请求Rpc类(一般由candidate发起)
 * @author yiyewei
 * @create 2022/9/20 15:23
 **/
public class RequestVoteRpc {

    //自己当前任期
    private int term;

    //候选者ID，一般是发送者自己
    private NodeId candidateId;

    //候选者最后一条日志的索引号
    private int lastLogIndex=0;

    //候选者最后一条日志的任期
    private int lastLogTerm=0;

    public int getTerm() {
        return term;
    }

    public void setTerm(int term) {
        this.term = term;
    }

    public NodeId getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(NodeId candidateId) {
        this.candidateId = candidateId;
    }

    public int getLastLogIndex() {
        return lastLogIndex;
    }

    public void setLastLogIndex(int lastLogIndex) {
        this.lastLogIndex = lastLogIndex;
    }

    public int getLastLogTerm() {
        return lastLogTerm;
    }

    public void setLastLogTerm(int lastLogTerm) {
        this.lastLogTerm = lastLogTerm;
    }

    @Override
    public String toString() {
        return "RequestVoteRpc{" +
                "term=" + term +
                ", candidateId=" + candidateId +
                ", lastLogIndex=" + lastLogIndex +
                ", lastLogTerm=" + lastLogTerm +
                '}';
    }
}
