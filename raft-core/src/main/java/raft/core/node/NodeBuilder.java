package raft.core.node;

import com.google.common.base.Preconditions;
import com.google.common.eventbus.EventBus;
import io.netty.channel.nio.NioEventLoopGroup;
import raft.core.log.Log;
import raft.core.log.MemoryLog;
import raft.core.node.base.NodeEndpoint;
import raft.core.node.base.NodeGroup;
import raft.core.node.base.NodeId;
import raft.core.node.store.MemoryNodeStore;
import raft.core.node.store.NodeStore;
import raft.core.rpc.Connector;
import raft.core.rpc.nio.NioConnector;
import raft.core.scheduler.DefaultScheduler;
import raft.core.scheduler.Scheduler;
import raft.core.support.SingleThreadTaskExecutor;
import raft.core.support.TaskExecutor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.Collection;
import java.util.Collections;

public class NodeBuilder {

    //成员列表
    private NodeGroup group;

    //当前节点ID
    private NodeId selfId;

    private EventBus eventBus;

    //定时器组件
    private Scheduler scheduler = null;

    //RPC组件
    private Connector connector = null;

    //主线程执行器
    private TaskExecutor taskExecutor = null;

    //部分状态持久化
    private NodeStore store = null;

    private Log log = null;

    private boolean standby = false;

    private TaskExecutor groupConfigChangeTaskExecutor = null;


    private NioEventLoopGroup workerNioEventLoopGroup = null;

    //单节点构造器
    public NodeBuilder(NodeEndpoint endpoint) {
        this(Collections.singletonList(endpoint), endpoint.getId());
    }

    //多节点构造器
    public NodeBuilder(Collection<NodeEndpoint> endpoints, NodeId selfId) {
        this.group = new NodeGroup(endpoints, selfId);
        this.selfId = selfId;
        this.eventBus = new EventBus(selfId.getValue());
    }

    public NodeBuilder setScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
        return this;
    }

    public NodeBuilder setConnector(Connector connector) {
        this.connector = connector;
        return this;
    }

    public NodeBuilder setTaskExecutor(TaskExecutor taskExecutor) {
        this.taskExecutor = taskExecutor;
        return this;
    }

    public NodeBuilder setStore(@Nonnull NodeStore store) {
        Preconditions.checkNotNull(store);
        this.store = store;
        return this;
    }

    public NodeBuilder setLog(Log log) {
        this.log = log;
        return this;
    }


    //创建入口
    public Node build() {
        return new NodeImpl(buildContext());
    }

    //创建上下文
    private NodeContext buildContext() {
        NodeContext context = new NodeContext();
        context.setGroup(group);
        context.setLog(log != null ? log : new MemoryLog(eventBus));
        context.setStore(store != null ? store : new MemoryNodeStore());
        context.setSelfId(selfId);
        context.setEventBus(eventBus);
        context.setScheduler(scheduler != null ? scheduler : new DefaultScheduler(3000, 4000, 0, 1000));
        context.setConnector(connector != null ? connector : createNioConnector());
        context.setTaskExecutor(taskExecutor != null ? taskExecutor : new SingleThreadTaskExecutor("node"));

        return context;
    }

    private NioConnector createNioConnector() {
        int port = group.findSelf().getEndpoint().getPort();
        if (workerNioEventLoopGroup != null) {
            return new NioConnector(workerNioEventLoopGroup, selfId, eventBus, port, 1000);
        }
        return new NioConnector(new NioEventLoopGroup(10), false, selfId, eventBus, port, 1000);
    }

    /**
     * Set data directory.
     *
     * @param dataDirPath data directory
     * @return this
     */
    public NodeBuilder setDataDir(@Nullable String dataDirPath) {
        if (dataDirPath == null || dataDirPath.isEmpty()) {
            return this;
        }
        File dataDir = new File(dataDirPath);
        if (!dataDir.isDirectory() || !dataDir.exists()) {
            throw new IllegalArgumentException("[" + dataDirPath + "] not a directory, or not exists");
        }
//        log = new FileLog(dataDir, eventBus);
//        store = new FileNodeStore(new File(dataDir, FileNodeStore.FILE_NAME));
        return this;
    }

    /**
     * Set standby.
     *
     * @param standby standby
     * @return this
     */
    public NodeBuilder setStandby(boolean standby) {
        this.standby = standby;
        return this;
    }

}
