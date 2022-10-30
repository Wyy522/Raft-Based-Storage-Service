package raft.core.scheduler;

import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.Random;
import java.util.concurrent.*;

/**
 * 默认定时器实现类
 * @author yiyewei
 * @create 2022/9/20 10:37
 **/

public class DefaultScheduler implements Scheduler{

    private static final Logger logger = LoggerFactory.getLogger(DefaultScheduler.class);

    //最小选举超时时间
    private final int minElectionTimeout;

    //最大选举超时时间
    private final int maxElectionTimeout;

    //初次日志复制延迟时间
    private final int logReplicationDelay;

    //日志复制间隔
    private final int logReplicationInterval;

    //随机数生成器
    private final Random electionTimeoutRandom;

    //定时调度器
    private final ScheduledExecutorService scheduledExecutorService;

    //构造函数
    public DefaultScheduler(int minElectionTimeout, int maxElectionTimeout,
                            int logReplicationDelay, int logReplicationInterval) {

        //判断参数是否有效
        //最小和最大选举超时间隔
        if (minElectionTimeout<=0||maxElectionTimeout<=0||minElectionTimeout>maxElectionTimeout){
            throw new IllegalArgumentException("election timeout should not be 0 or min > max");
        }

        //初次日志复制延迟以及时间间隔
        if (logReplicationDelay<0||logReplicationInterval<=0){
            throw new IllegalArgumentException("log replication delay < 0 or log replication interval <= 0");
        }

        this.minElectionTimeout = minElectionTimeout;
        this.maxElectionTimeout = maxElectionTimeout;
        this.logReplicationDelay = logReplicationDelay;
        this.logReplicationInterval = logReplicationInterval;
        this.electionTimeoutRandom = new Random();

        //该线程池只有一个核心线程,队列实现的是DelayQueue,也就是说同一时刻只有一个线程会执行队列里的方法
        this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r,"scheduler"));
    }

//    //创建日志复制定时器(一个任务完成logReplicationInterval秒后再次运行)
//    @Override
//    public LogReplicationTask scheduleLogReplicationTask(Runnable task) {
//
//        ScheduledFuture<?> scheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(task, logReplicationDelay,
//                logReplicationInterval, TimeUnit.MILLISECONDS);
//
//        return new LogReplicationTask(scheduledFuture);
//    }
//
//    //创建选举超时定时器(timeout后执行一次)
//    @Override
//    public ElectionTimeout scheduleElectionTimeout(Runnable task) {
//        //随机超时时间
//       int timeout =electionTimeoutRandom.nextInt(
//               maxElectionTimeout-minElectionTimeout)+maxElectionTimeout;
//        ScheduledFuture<?> scheduledFuture = scheduledExecutorService.schedule(task, timeout, TimeUnit.MILLISECONDS);
//
//        return new ElectionTimeout(scheduledFuture);
//    }

    @Override
    public LogReplicationTask scheduleLogReplicationTask( Runnable task) {
        Preconditions.checkNotNull(task);
        logger.debug("schedule log replication task");
        // 初始复制延迟
        ScheduledFuture<?> scheduledFuture = this.scheduledExecutorService.scheduleWithFixedDelay(
                task, logReplicationDelay, logReplicationInterval, TimeUnit.MILLISECONDS);
        return new LogReplicationTask(scheduledFuture);
    }

    @Override
    public ElectionTimeout scheduleElectionTimeout( Runnable task) {
        Preconditions.checkNotNull(task);
        logger.debug("schedule election timeout");
        int timeout = electionTimeoutRandom.nextInt(maxElectionTimeout - minElectionTimeout) + minElectionTimeout;
        ScheduledFuture<?> scheduledFuture = scheduledExecutorService.schedule(task, timeout, TimeUnit.MILLISECONDS);
        return new ElectionTimeout(scheduledFuture);
    }

    @Override
    public void stop() throws InterruptedException {
        logger.debug("stop scheduler");
        scheduledExecutorService.shutdown();
        scheduledExecutorService.awaitTermination(1, TimeUnit.SECONDS);

    }

   }
