package raft.core.node.store;

import raft.core.node.base.NodeId;

/**
 * 基于内存的状态存储
 * @author yiyewei
 * @create 2022/9/20 23:51
 **/
public class MemoryNodeStore implements NodeStore {

    //当前任期号
    private int term;

    //投票给谁
    private NodeId votedFor;

    public MemoryNodeStore(){
        this(0,null);
    }

    public MemoryNodeStore(int term, NodeId votedFor) {
        this.term = term;
        this.votedFor = votedFor;
    }

    @Override
    public int getTerm() {
        return term;
    }

    @Override
    public void setTerm(int term) {
        this.term=term;
    }

    @Override
    public NodeId getVotedFor() {
        return votedFor;
    }

    @Override
    public void setVotedFor(NodeId votedFor) {
        this.votedFor=votedFor;
    }

    @Override
    public void close() {

    }
}
