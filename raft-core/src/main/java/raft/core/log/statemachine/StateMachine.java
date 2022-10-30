package raft.core.log.statemachine;

import raft.core.log.entry.Entry;

public interface StateMachine {

    //获取lastApplied
    int getLastApplied();

    // 应用日志 applyLog方法是日之子组件回调上层服务的主要方法，提供了状态机上下文，日志的索引，命令的二进制数据
    // 以及第一条日志的索引，上层服务在applyLog方法的实现中应用命令，修改数据，并进行回复客户端等操作
    void applyLog(int index,byte[] commandBytes,int firstLogIndex);

    /**
     * 关闭状态机
     */
    void shutdown();

}
