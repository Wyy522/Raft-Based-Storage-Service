package raft.core.support;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FutureCallback;
import io.netty.util.concurrent.DefaultThreadFactory;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.*;

/**
 * 单线程执行任务
 * @author yiyewei
 * @create 2022/9/20 23:46
 **/
public class SingleThreadTaskExecutor implements TaskExecutor{

    private final ExecutorService executorService;

    //构造器,指定名称
    public SingleThreadTaskExecutor(String name) {
        this(r -> new Thread(r,name));
    }

    //构造器,指定ThreadFactory
    private SingleThreadTaskExecutor(ThreadFactory threadFactory){
       executorService = Executors.newSingleThreadExecutor(threadFactory);
    }

    //提交任务并执行
    @Override
    public Future<?> submit(Runnable task) {
        return executorService.submit(task);
    }

    //提交任务并执行,有返回值
    @Override
    public <V> Future<V> submit(Callable<V> task) {
        return executorService.submit(task);
    }

    //关闭任务执行器
    @Override
    public void shutdown() throws InterruptedException {
        executorService.shutdown();
        executorService.awaitTermination(1,TimeUnit.SECONDS);
    }

    @Override
    public void submit(@Nonnull Runnable task, @Nonnull Collection<FutureCallback<Object>> callbacks) {
        Preconditions.checkNotNull(task);
        Preconditions.checkNotNull(callbacks);
        executorService.submit(() -> {
            try {
                task.run();
                callbacks.forEach(c -> c.onSuccess(null));
            } catch (Exception e) {
                callbacks.forEach(c -> c.onFailure(e));
            }
        });
    }


    @Override
    public void submit(@Nonnull Runnable task, @Nonnull FutureCallback<Object> callback) {
        Preconditions.checkNotNull(task);
        Preconditions.checkNotNull(callback);
        submit(task, Collections.singletonList(callback));
    }
}
