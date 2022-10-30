package raft.core.node.role;

import raft.core.node.base.NodeId;
import raft.core.scheduler.LogReplicationTask;

public class LeaderNodeRole extends AbstractNodeRole{

    //日志复制定时器
    private final LogReplicationTask logReplicationTask;

    //leader构造器
    public LeaderNodeRole(int term, LogReplicationTask logReplicationTask) {
        super(RoleName.LEADER, term);
        this.logReplicationTask = logReplicationTask;
    }





    //取消日志复制定时任务
    @Override
    public void cancelTimeoutTask() {
        logReplicationTask.cancel();
    }

    @Override
    public NodeId getLeaderId(NodeId selfId) {
       return selfId;
    }

    @Override
    public String toString() {
        return "LeaderNodeRole{" +
                "term=" + term +
                ", logReplicationTask=" + logReplicationTask +
                '}';
    }
}
