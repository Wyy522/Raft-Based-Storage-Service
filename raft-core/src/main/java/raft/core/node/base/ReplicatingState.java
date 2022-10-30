package raft.core.node.base;

/**
 * 日志复制状态
 * @author yiyewei
 * @create 2022/9/19 20:42
 **/
public class ReplicatingState {
    private int nextIndex;
    private int matchIndex;

    ReplicatingState(int nextIndex) {
        this(nextIndex, 0);
    }

    ReplicatingState(int nextIndex, int matchIndex) {
        this.nextIndex = nextIndex;
        this.matchIndex = matchIndex;
    }

    boolean advance(int lastEntryIndex) {
        // changed
        // 说明 matchIndex和nextIndex可以改变了
        boolean result = (matchIndex != lastEntryIndex || nextIndex != (lastEntryIndex + 1));

        matchIndex = lastEntryIndex;
        nextIndex = lastEntryIndex + 1;
        return result;
    }

    boolean backOffNextIndex() {
        if (nextIndex > 1) {
            nextIndex--;
            return true;
        }
        return false;
    }

    int getNextIndex() {
        return nextIndex;
    }


    int getMatchIndex() {
        return matchIndex;
    }
}
