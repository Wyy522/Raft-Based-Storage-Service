package raft.kvstore.message;


import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;

public class CommandRequest<T> {

    private final T command;

    private final Channel channel;

    public CommandRequest(T command, Channel channel) {
        this.command = command;
        this.channel = channel;
    }

    //响应结果
    public void reply(Object response){
        this.channel.writeAndFlush(response);
    }

    //关闭时的监听器
    public void addCloseListener(Runnable runnable){
        this.channel.closeFuture().addListener(
                (ChannelFutureListener) future->runnable.run()
        );
    }

    //获取命令
    public T getCommand(){
        return  command;
    }


}
