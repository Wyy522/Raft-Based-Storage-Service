package raft.core.node.base;

/**
 * 集群成员信息类
 * @author yiyewei
 * @create 2022/9/19 20:38
 **/
public class GroupMember {
    //节点服务器IP和端口
    private final NodeEndpoint endpoint;

    //日志复制进度
    private ReplicatingState replicatingState;

    //无日志复制状态的构造函数
    public GroupMember(NodeEndpoint endpoint) {
        this(endpoint,null);
    }

    //带日志复制状态的构造函数
    public GroupMember(NodeEndpoint endpoint, ReplicatingState replicatingState) {
        this.endpoint = endpoint;
        this.replicatingState = replicatingState;
    }

    //获取当前节点信息
    public NodeEndpoint getEndpoint() {
        return endpoint;
    }

    //获取当前节点日志复制进度
    public ReplicatingState getReplicatingState() {
        return replicatingState;
    }

    //设置当前节点日志复制进度
    public void setReplicatingState(ReplicatingState replicatingState) {
        this.replicatingState = replicatingState;
    }

    //获取nextIndex
    public int getNextIndex(){
        return ensureReplicatingState().getNextIndex();
    }

    //获取matchIndex
    public int getMatchIndex(){
        return ensureReplicatingState().getMatchIndex();
    }

    //获取当前节点复制进度
    private ReplicatingState ensureReplicatingState(){
        if(replicatingState==null){
            throw  new IllegalStateException("replication state not set");
        }
        return replicatingState;
    }

    public NodeId getId() {
        return endpoint.getId();
    }
    boolean idEquals(NodeId id) {
        return endpoint.getId().equals(id);
    }

    public boolean advanceReplicatingState(int lastEntryIndex) {
        return ensureReplicatingState().advance(lastEntryIndex);
    }

    public boolean backOffNextIndex() {
        return ensureReplicatingState().backOffNextIndex();
    }


}
