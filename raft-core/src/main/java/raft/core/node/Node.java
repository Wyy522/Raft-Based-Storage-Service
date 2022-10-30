package raft.core.node;

import raft.core.log.statemachine.StateMachine;
import raft.core.node.role.RoleNameAndLeaderId;

/**
 * 给上层服务的接口
 * @author yiyewei
 * @create 2022/9/21 18:52
 **/
public interface Node {
    //启动
    void start();

    //关闭
    void stop() throws InterruptedException;

    //注册状态机
    void registerStateMachine(StateMachine stateMachine);

    //追加日志
    void appendLog(byte[] commandBytes);

    byte[] getLogByKey(String key);

    RoleNameAndLeaderId getRoleNameAndLeaderId() ;
}
