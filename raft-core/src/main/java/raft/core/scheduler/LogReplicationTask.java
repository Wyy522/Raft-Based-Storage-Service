package raft.core.scheduler;

import java.util.concurrent.ScheduledFuture;

/**
 * 日志复制任务
 * @author yiyewei
 * @create 2022/9/20 10:37
 **/
public class LogReplicationTask {

    private final ScheduledFuture<?> scheduledFuture;

    //表示不设置日志复制
    public static final LogReplicationTask NONE = new LogReplicationTask(new NullScheduledFuture());


    //构造器
    public LogReplicationTask(ScheduledFuture<?> scheduledFuture) {
        this.scheduledFuture = scheduledFuture;
    }

    //取消日志复制定时器
    public void cancel(){
        this.scheduledFuture.cancel(false);
    }

    @Override
    public String toString() {
        return "LogReplicationTask{" +
                "scheduledFuture=" + scheduledFuture +
                '}';
    }
}
