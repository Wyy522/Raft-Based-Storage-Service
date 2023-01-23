package raft.core.node;

import com.google.common.eventbus.EventBus;
import raft.core.log.Log;
import raft.core.node.base.GroupMember;
import raft.core.node.base.NodeGroup;
import raft.core.node.base.NodeId;
import raft.core.node.store.NodeStore;
import raft.core.rpc.Connector;
import raft.core.scheduler.Scheduler;
import raft.core.support.TaskExecutor;

/**
 * 节点上下文环境
 * @author yiyewei
 * @create 2022/9/21 19:27
 **/

public class NodeContext {

    //当前节点ID
    private NodeId selfId;

    //成员列表
    private NodeGroup group;

    //RPC组件
    private Connector connector;

    //定时器组件
    private Scheduler scheduler;

    private EventBus eventBus;

    //主线程执行器
    private TaskExecutor taskExecutor;

    //部分角色状态数据存储
    private NodeStore store;

    //日志
    private Log log;

    public GroupMember findMember(NodeId id){
        return  group.findMember(id);
    }

    public EventBus eventBus() {
        return eventBus;
    }

    public Connector connector(){
        return connector;
    }

    public NodeStore store(){
        return store;
    }

    public Scheduler scheduler(){
        return scheduler;
    }

    public NodeGroup group(){
        return group;
    }

    public TaskExecutor taskExecutor(){ return taskExecutor;}

    public NodeId selfId() {
        return selfId;
    }

    public NodeId getSelfId() {
        return selfId;
    }

    public void setSelfId(NodeId selfId) {
        this.selfId = selfId;
    }

    public NodeGroup getGroup() {
        return group;
    }

    public void setGroup(NodeGroup group) {
        this.group = group;
    }

    public Connector getConnector() {
        return connector;
    }

    public void setConnector(Connector connector) {
        this.connector = connector;
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    public void setScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    public EventBus getEventBus() {
        return eventBus;
    }

    public void setEventBus(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    public TaskExecutor getTaskExecutor() {
        return taskExecutor;
    }

    public void setTaskExecutor(TaskExecutor taskExecutor) {
        this.taskExecutor = taskExecutor;
    }

    public NodeStore getStore() {
        return store;
    }

    public void setStore(NodeStore store) {
        this.store = store;
    }

    public Log getLog() {
        return log;
    }

    public void setLog(Log log) {
        this.log = log;
    }

    public Log log() {
        return log;
    }
}
