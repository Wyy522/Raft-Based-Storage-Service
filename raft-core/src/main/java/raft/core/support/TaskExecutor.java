package raft.core.support;

import com.google.common.util.concurrent.FutureCallback;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * 任务执行接口
 * @author yiyewei
 * @create 2022/9/20 16:16
 **/
public interface TaskExecutor {


    //提交任务
    Future<?> submit(Runnable task);

    void submit( Runnable task,  FutureCallback<Object> callback);

    void submit( Runnable task,  Collection<FutureCallback<Object>> callbacks);

    //提交任务,任务有返回值
    <V> Future<V> submit(Callable<V> task);

    //关闭任务执行器
    void shutdown() throws InterruptedException;
}
