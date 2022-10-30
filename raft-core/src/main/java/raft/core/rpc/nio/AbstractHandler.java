package raft.core.rpc.nio;

import com.google.common.eventbus.EventBus;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import raft.core.node.base.NodeId;
import raft.core.rpc.Channel;
import raft.core.rpc.message.*;

import java.util.Objects;

public class AbstractHandler extends ChannelDuplexHandler {

    private static final Logger logger = LoggerFactory.getLogger(AbstractHandler.class);

    protected final EventBus eventBus;

    //远程节点ID
    NodeId remoteId;

    //RPC组件中的Channel,并非Netty的Channel
    protected Channel channel;

    //最后发送的AppendEntriesRpc消息
    private AppendEntriesRpc lastAppendEntriesRpc;

    public AbstractHandler(EventBus eventBus) {
        this.eventBus=eventBus;
    }


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        assert remoteId != null;
        assert channel != null;
        if (msg instanceof RequestVoteRpc) {
            RequestVoteRpc rpc = (RequestVoteRpc) msg;
            eventBus.post(new RequestVoteRpcMessage(rpc, remoteId, channel));
        } else if (msg instanceof RequestVoteResult) {
            eventBus.post(msg);
        } else if (msg instanceof AppendEntriesRpc) {
            AppendEntriesRpc rpc = (AppendEntriesRpc) msg;
            eventBus.post(new AppendEntriesRpcMessage(rpc, remoteId, channel));
        } else if (msg instanceof AppendEntriesResult) {
            AppendEntriesResult result = (AppendEntriesResult) msg;
            if (lastAppendEntriesRpc == null) {
                logger.warn("no last append entries rpc");
            } else {
                if (!Objects.equals(result.getRpcMessageId(), lastAppendEntriesRpc.getMessageId())) {
                    logger.warn("incorrect append entries rpc message id {}, expected {}", result.getRpcMessageId(), lastAppendEntriesRpc.getMessageId());
                } else {
                    eventBus.post(new AppendEntriesResultMessage(result, remoteId, lastAppendEntriesRpc));
                    lastAppendEntriesRpc = null;
                }
            }
        }
    }

    //发送前记录最后一个AppendEntries消息
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof AppendEntriesRpc){
            lastAppendEntriesRpc=(AppendEntriesRpc) msg;
        }
        super.write(ctx,msg,promise);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.warn(cause.getMessage(), cause);
        ctx.close();
    }
}
