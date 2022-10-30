package raft.core.node.role;

import raft.core.node.base.NodeId;

/**
 * 抽象节点角色类
 * @author yiyewei
 * @create 2022/9/19 21:58
 **/
public abstract class AbstractNodeRole {
    //角色名称
    private final RoleName name;
    //任期号 单调递增
    protected final int term;

    //构造函数
    public AbstractNodeRole(RoleName name, int term) {
        this.name = name;
        this.term = term;
    }

    //获取当前角色
    public RoleName getName(){
        return  name;
    }

    //获取当前的term
    public int getTerm(){
        return term;
    }

    //取消超时或定时任务,角色从一个角色切换到另一个角色时，必须取消当前的超时或者定时任务,然后创建新的
    public abstract void cancelTimeoutTask();

    //获取leaderId
    public abstract NodeId getLeaderId(NodeId selfId);

    public RoleNameAndLeaderId getNameAndLeaderId(NodeId selfId) {
        return new RoleNameAndLeaderId(name, getLeaderId(selfId));
    }

}
