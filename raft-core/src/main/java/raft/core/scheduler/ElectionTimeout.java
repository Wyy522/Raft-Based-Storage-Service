package raft.core.scheduler;

import java.util.concurrent.ScheduledFuture;

/**
 * 选举超时类
 * @author yiyewei
 * @create 2022/9/20 10:21
 **/
public class ElectionTimeout {

    private final ScheduledFuture<?> scheduledFuture;

    //表示不设置选举超时
    public static final ElectionTimeout NONE = new ElectionTimeout(new NullScheduledFuture());

    //构造器
    public ElectionTimeout(ScheduledFuture<?> scheduledFuture) {
        this.scheduledFuture = scheduledFuture;
    }

    //取消选举超时
    public void cancel(){
        this.scheduledFuture.cancel(false);
    }

    @Override
    public String toString() {

        //选举超时已取消
        if (this.scheduledFuture.isCancelled()){
            return "ElectionTimeout(state=cancelled)";
        }

        //选举超时已执行
        if (this.scheduledFuture.isDone()){
            return "ElectionTimeout(state=done)";
        }

        //选举超时尚未执行，在多少毫秒后执行
        return "ElectionTimeout{" +
                "scheduledFuture=" + scheduledFuture +
                '}';
    }
}
