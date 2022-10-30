package raft.core.node.store;

import raft.core.node.base.NodeId;

/**
 * term和votedFor状态存储
 * @author yiyewei
 * @create 2022/9/20 23:22
 **/
public interface NodeStore {

    //获取currentTerm
    int getTerm();

    //设置currentTerm
    void setTerm(int term);

    //获取voteFor
    NodeId getVotedFor();

    //设置voteFor
    void setVotedFor(NodeId votedFor);

    //关闭文件
    void close();
}
