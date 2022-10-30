package raft.core.rpc.nio;

import com.google.common.eventbus.EventBus;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import raft.core.node.base.NodeId;

//接受来自外部连接时的处理器
public class FromRemoteHandler extends  AbstractHandler{

    private static final Logger logger = LoggerFactory.getLogger(FromRemoteHandler.class);

    //入口连接组
    private final InboundChannelGroup channelGroup;

    public FromRemoteHandler(EventBus eventBus,InboundChannelGroup channelGroup) {
        super(eventBus);
        this.channelGroup = channelGroup;
    }

    //Decoder解析完成后被调用
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof NodeId){
            remoteId =(NodeId) msg;
            NioChannel nioChannel = new NioChannel(ctx.channel());
            channel=nioChannel;
            channelGroup.add(remoteId,nioChannel);
            return;
        }

        logger.debug("receive {} from {}", msg, remoteId);
        super.channelRead(ctx, msg);
    }
}
