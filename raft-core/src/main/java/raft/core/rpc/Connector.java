package raft.core.rpc;

import raft.core.node.base.NodeEndpoint;
import raft.core.rpc.message.*;

import java.util.Collection;

/**
 * Rpc接口
 * @author yiyewei
 * @create 2022/9/20 16:14
 **/
public interface Connector {

    //初始化连接器
    void initialize();

    //发送投票请求Rpc给多个节点,群发,所以是个集合
    void sendRequestVote(RequestVoteRpc rpc, Collection<NodeEndpoint> destinationEndpoints);

    //回复投票请求Rpc的结果给单个节点
//    void replyRequestVote(RequestVoteResult result, NodeEndpoint destinationEndpoint);

    void replyRequestVote( RequestVoteResult result,  RequestVoteRpcMessage rpcMessage);

    //发送日志追加请求给单个节点
    void sendAppendEntries(AppendEntriesRpc rpc, NodeEndpoint destinationEndpoint);

    //回复日志追加请求结构给单个节点
//    void replyAppendEntries(AppendEntriesResult result , NodeEndpoint destinationEndpoint);

    void replyAppendEntries( AppendEntriesResult result,  AppendEntriesRpcMessage rpcMessage);

    void resetChannels();

    //关闭连接器
    void close();
}
