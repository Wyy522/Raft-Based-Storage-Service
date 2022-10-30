package raft.core.log.statemachine;


public class EmptyStateMachine implements StateMachine {

    private int lastApplied = 0;

    @Override
    public int getLastApplied() {
        return lastApplied;
    }

    @Override
    public void applyLog(int index, byte[] commandBytes, int firstLogIndex) {
        lastApplied = index;
    }

    @Override
    public void shutdown() {
    }

}
