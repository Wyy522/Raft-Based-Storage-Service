package raft.core.log.statemachine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import raft.core.log.execption.StateMachineException;
import raft.core.support.SingleThreadTaskExecutor;
import raft.core.support.TaskExecutor;

import javax.annotation.Nonnull;

public abstract class AbstractSingleThreadStateMachine implements StateMachine {

    private static final Logger logger = LoggerFactory.getLogger(AbstractSingleThreadStateMachine.class);

    private volatile int lastApplied=0;

    private final TaskExecutor taskExecutor;

    public AbstractSingleThreadStateMachine() {
        this.taskExecutor = new SingleThreadTaskExecutor("state-machine");
    }

    @Override
    public int getLastApplied() {
        return lastApplied;
    }

    //应用日志
    @Override
    public void applyLog(int index,  byte[] commandBytes, int firstLogIndex) {
        System.out.println("已注入");
        taskExecutor.submit(() -> doApplyLog( index, commandBytes, firstLogIndex));
//        doApplyLog(context, index, commandBytes, firstLogIndex);
    }

    //应用日志(TaskExecutor的线程中)
    private void doApplyLog(int index,byte[] commandBytes,int firstLogIndex ){
        // 忽略已经应用过的日志
        if (index <= lastApplied){
            return;
        }
        logger.debug("apply log {}", index);
        this.applyCommand(commandBytes);
        System.out.println("state machine 已同步");
        // 跟新lastApplied
        lastApplied = index;

    }

    /**
     * // 应用命令抽象方法
     * @param commandBytes
     */
    protected abstract void applyCommand( byte[] commandBytes);

    //关闭状态机
    @Override
    public void shutdown() {
        try {
            taskExecutor.shutdown();
        } catch (InterruptedException e) {
            throw new StateMachineException(e);
        }
    }
}
