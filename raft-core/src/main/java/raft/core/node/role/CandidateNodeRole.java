package raft.core.node.role;

import raft.core.node.base.NodeId;
import raft.core.scheduler.ElectionTimeout;

public class CandidateNodeRole extends AbstractNodeRole{

    //candidate得票数
    private final int votesCount;

    //选举超时
    private final ElectionTimeout electionTimeout;

    //candidate构造器,票数为1
    public CandidateNodeRole(int term, ElectionTimeout electionTimeout) {
        this(term,1,electionTimeout);
    }

    //candidate构造器，可指定票数
    public CandidateNodeRole(int term, int votesCount, ElectionTimeout electionTimeout) {
        super(RoleName.CANDIDATE, term);
        this.votesCount = votesCount;
        this.electionTimeout = electionTimeout;
    }

    //取消选举超时
    @Override
    public void cancelTimeoutTask() {
        electionTimeout.cancel();
    }

    @Override
    public NodeId getLeaderId(NodeId selfId) {
        return null;
    }

    //？？
    public CandidateNodeRole increaseVotesCount(ElectionTimeout electionTimeout){
        this.electionTimeout.cancel();
        return new CandidateNodeRole(term,votesCount+1,electionTimeout);
    }

    //获取投票数
    public int getVotesCount() {
        return votesCount;
    }


    @Override
    public String toString() {
        return "CandidateNodeRole{" +
                "term=" + term +
                ", votesCount=" + votesCount +
                ", electionTimeout=" + electionTimeout +
                '}';
    }
}
