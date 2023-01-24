package raft.core.log;


import raft.core.log.statemachine.StateMachine;
import raft.core.node.base.NodeId;
import raft.core.log.entry.Entry;
import raft.core.log.entry.EntryMeta;
import raft.core.log.entry.GeneralEntry;
import raft.core.log.entry.NoOpEntry;
import raft.core.rpc.message.AppendEntriesRpc;

import java.util.List;

/**
 * 日志接口
 * @author yiyewei
 * @create 2022/9/24 23:46
 **/
public interface Log {

    int ALL_ENTRIES = -1;

    //获取最后一条日志的元信息(term,index)
    EntryMeta getLastEntryMeta();

    //创建AppendEntries消息
    AppendEntriesRpc createAppendEntriesRpc(int term, NodeId selfId, int nextIndex, int maxEntries);

    //增加一个NO-OP日志
    NoOpEntry appendEntry(int term);

    //增加一个普通日志
    GeneralEntry appendEntry(int term, byte[] command);

    //追加来自Leader的日志
    boolean appendEntriesFromLeader(int prevLogIndex, int prevLogTerm, List<Entry> entries);

    //推进已提交的日志索引
    void advanceCommitIndex(int newCommitIndex, int currentTerm);

    //获取下一条日志的索引
    int getNextIndex();

    //获取当前已提交的日志索引
    int getCommitIndex();

    //判断LastLogIndex和LastLogTerm是否比自己新
    boolean isNewerThan(int lastLogIndex, int lastLogTerm);

    void setStateMachine(StateMachine stateMachine);

    byte[] getLogByKey(String key);

    //关闭
    void close();

}
