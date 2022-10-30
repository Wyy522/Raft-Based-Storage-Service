package raft.core.node.role;

import raft.core.node.base.NodeId;
import raft.core.scheduler.ElectionTimeout;

/**
 * Follower节点
 * @author yiyewei
 * @create 2022/9/20 10:20
 **/
public class FollowerNodeRole extends AbstractNodeRole{

    private final NodeId votedFor;//投过票的节点id,可能为空
    private final NodeId leaderId;//当前leader节点Id,可能为空
    private final ElectionTimeout electionTimeout;//选举超时

    //follower构造器
    public FollowerNodeRole(int term, NodeId votedFor, NodeId leaderId, ElectionTimeout electionTimeout) {
        super(RoleName.FOLLOWER, term);
        this.votedFor = votedFor;
        this.leaderId = leaderId;
        this.electionTimeout = electionTimeout;
    }

    //获取投过票的节点
    public NodeId getVotedFor() {
        return votedFor;
    }

    //获取当前leader的ID
    public NodeId getLeaderId() {
        return leaderId;
    }

    //取消选举超时
    @Override
    public void cancelTimeoutTask() {
        electionTimeout.cancel();
    }

    @Override
    public NodeId getLeaderId(NodeId selfId) {
        return leaderId;
    }

    @Override
    public String toString() {
        return "FollowerNodeRole{" +
                "term=" + term +
                ", votedFor=" + votedFor +
                ", leaderId=" + leaderId +
                ", electionTimeout=" + electionTimeout +
                '}';
    }
}
