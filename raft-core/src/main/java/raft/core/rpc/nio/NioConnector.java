package raft.core.rpc.nio;

import com.google.common.base.Preconditions;
import com.google.common.eventbus.EventBus;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import raft.core.node.base.NodeEndpoint;
import raft.core.node.base.NodeId;
import raft.core.rpc.Channel;
import raft.core.rpc.exception.ChannelConnectException;
import raft.core.rpc.Connector;
import raft.core.rpc.exception.ConnectorException;
import raft.core.rpc.message.*;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NioConnector implements Connector {

    private static final Logger logger = LoggerFactory.getLogger(NioConnector.class);
    private final NioEventLoopGroup bossNioEventLoopGroup = new NioEventLoopGroup(1);
    private final NioEventLoopGroup workerNioEventLoopGroup;
    // 解决核心组件和通信组件之间双向依赖问题的pubsub工具
    private final EventBus eventBus;
    // 节点间通信端口
    private final int port;
    private final InboundChannelGroup inboundChannelGroup = new InboundChannelGroup();
    private final OutboundChannelGroup outboundChannelGroup;

    private final ExecutorService executorService = Executors.newCachedThreadPool((r) -> {
        Thread thread = new Thread(r);
        thread.setUncaughtExceptionHandler((t, e) -> {
            logException(e);
        });
        return thread;
    });

    //初始化连接器信息，包括工作线程为10，工作线程不共享，当前连接器的对应节点的ID，eventBUS,服务端口，日志复制间隔
    public NioConnector(NioEventLoopGroup workerNioEventLoopGroup,
                        NodeId selfNodeId, EventBus eventBus,
                        int port, int logReplicationInterval) {
        // 复用工作线程池
        this.workerNioEventLoopGroup = workerNioEventLoopGroup;
        this.eventBus = eventBus;
        this.port = port;
        outboundChannelGroup = new OutboundChannelGroup(workerNioEventLoopGroup, eventBus, selfNodeId, logReplicationInterval);
    }

    //2333
    @Override
    public void initialize() {
        ServerBootstrap serverBootstrap = new ServerBootstrap()
                .group(bossNioEventLoopGroup, workerNioEventLoopGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new Decoder());
                        pipeline.addLast(new Encoder());
                        pipeline.addLast(new FromRemoteHandler(eventBus, inboundChannelGroup));
                    }
                });
        logger.debug("node listen on port {}", port);
        try {
            serverBootstrap.bind(port).sync();
        } catch (InterruptedException e) {
            throw new ConnectorException("failed to bind port", e);
        }
    }

    @Override
    public void sendRequestVote( RequestVoteRpc rpc,  Collection<NodeEndpoint> destinationEndpoints) {
        Preconditions.checkNotNull(rpc);
        Preconditions.checkNotNull(destinationEndpoints);
        for (NodeEndpoint endpoint : destinationEndpoints) {
            logger.debug("send {} to node {}", rpc, endpoint.getId());
            executorService.execute(() -> getChannel(endpoint).writeRequestVoteRpc(rpc));
        }
    }

    private Channel getChannel(NodeEndpoint endpoint) {
        // 按照节点id创建或者获取 连接 对端节点的channel
        return outboundChannelGroup.getOrConnect(endpoint.getId(), endpoint.getAddress());
    }
    private void logException(Throwable e) {
        if (e instanceof ChannelConnectException) {
            logger.warn(e.getMessage());
        } else {
            logger.warn("failed to process channel", e);
        }
    }

    @Override
    public void replyRequestVote(@Nonnull RequestVoteResult result, @Nonnull RequestVoteRpcMessage rpcMessage) {
        Preconditions.checkNotNull(result);
        Preconditions.checkNotNull(rpcMessage);
        logger.debug("reply {} to node {}", result, rpcMessage.getSourceNodeId());
        try {
            rpcMessage.getChannel().writeRequestVoteResult(result);
        } catch (Exception e) {
            logException(e);
        }
    }



    @Override
    public void sendAppendEntries(@Nonnull AppendEntriesRpc rpc, @Nonnull NodeEndpoint destinationEndpoint) {
        Preconditions.checkNotNull(rpc);
        Preconditions.checkNotNull(destinationEndpoint);
        logger.debug("send {} to node {}", rpc, destinationEndpoint.getId());
        executorService.execute(() -> getChannel(destinationEndpoint).writeAppendEntriesRpc(rpc));
    }

//    @Override
//    public void replyAppendEntries(@Nonnull AppendEntriesResult result, @Nonnull NodeEndpoint destinationEndpoint) {
//
//    }

//    @Override
//    public void replyRequestVote(@Nonnull RequestVoteResult result, @Nonnull NodeEndpoint destinationEndpoint) {
//
//    }

    @Override
    public void replyAppendEntries(@Nonnull AppendEntriesResult result, @Nonnull AppendEntriesRpcMessage rpcMessage) {
        Preconditions.checkNotNull(result);
        Preconditions.checkNotNull(rpcMessage);
        logger.debug("reply {} to node {}", result, rpcMessage.getSourceNodeId());
        try {
            rpcMessage.getChannel().writeAppendEntriesResult(result);
        } catch (Exception e) {
            logException(e);
        }
    }

    @Override
    public void resetChannels() {
        inboundChannelGroup.closeAll();
    }



    @Override
    public void close() {
        logger.debug("close connector");
        inboundChannelGroup.closeAll();
        outboundChannelGroup.closeAll();
        bossNioEventLoopGroup.shutdownGracefully();
        workerNioEventLoopGroup.shutdownGracefully();
    }
}
