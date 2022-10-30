package raft.core.scheduler;

/**
 * 基础定时器接口
 * @author yiyewei
 * @create 2022/9/20 10:12
 **/
public interface Scheduler {
    //创建日志复制定时任务
    LogReplicationTask scheduleLogReplicationTask(Runnable task);

    //创建选举超时器
    ElectionTimeout scheduleElectionTimeout(Runnable task);

    //关闭定时器
    void stop() throws InterruptedException;
}
