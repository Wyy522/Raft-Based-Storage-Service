package raft.core.rpc.nio;


import raft.core.rpc.Channel;
import raft.core.rpc.exception.ChannelException;
import raft.core.rpc.message.AppendEntriesResult;
import raft.core.rpc.message.AppendEntriesRpc;
import raft.core.rpc.message.RequestVoteResult;
import raft.core.rpc.message.RequestVoteRpc;

public class NioChannel implements Channel {
    private final io.netty.channel.Channel nettyChannel;

    NioChannel(io.netty.channel.Channel nettyChannel) {
        this.nettyChannel = nettyChannel;
    }

    @Override
    public void writeRequestVoteRpc( RequestVoteRpc rpc) {
        nettyChannel.writeAndFlush(rpc);
    }

    @Override
    public void writeRequestVoteResult( RequestVoteResult result) {
        nettyChannel.writeAndFlush(result);
    }

    @Override
    public void writeAppendEntriesRpc( AppendEntriesRpc rpc) {
        nettyChannel.writeAndFlush(rpc);
    }

    @Override
    public void writeAppendEntriesResult( AppendEntriesResult result) {
        nettyChannel.writeAndFlush(result);
    }



    @Override
    public void close() {
        try {
            nettyChannel.close().sync();
        } catch (InterruptedException e) {
            throw new ChannelException("failed to close", e);
        }
    }

    // 获取底层netty的channel
    io.netty.channel.Channel getDelegate() {
        return nettyChannel;
    }
}
