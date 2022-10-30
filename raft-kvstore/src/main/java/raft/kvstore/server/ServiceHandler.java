package raft.kvstore.server;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.concurrent.EventExecutorGroup;
import raft.kvstore.Protos;
import raft.kvstore.message.CommandRequest;
import raft.kvstore.message.GetCommand;
import raft.kvstore.message.SetCommand;

import java.util.Arrays;

public class ServiceHandler extends ChannelInboundHandlerAdapter {
    private final Service service;

    public ServiceHandler(Service service) {
        this.service = service;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        //分发命令
        if (msg instanceof GetCommand) {
            service.get(new CommandRequest<>((GetCommand) msg, ctx.channel())
            );
        } else if (msg instanceof SetCommand) {
//            System.out.println("Set Command  k: "+((SetCommand) msg).getKey()+"v:"+ Arrays.toString(((SetCommand) msg).getValue()));
            service.set(new CommandRequest<>((SetCommand) msg, ctx.channel())
            );
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

}
