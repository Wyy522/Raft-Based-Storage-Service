package raft.core.log;

import com.google.common.eventbus.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import raft.core.log.entry.Entry;
import raft.core.log.entry.EntryMeta;
import raft.core.log.entry.GeneralEntry;
import raft.core.log.entry.NoOpEntry;
import raft.core.log.execption.LogException;
import raft.core.log.sequence.EntrySequence;
import raft.core.log.statemachine.EmptyStateMachine;
import raft.core.log.statemachine.StateMachine;
import raft.core.node.base.NodeId;
import raft.core.rpc.message.AppendEntriesRpc;

import java.util.*;

abstract class AbstractLog implements Log {

    private static final Logger logger = LoggerFactory.getLogger(AbstractLog.class);

    protected final EventBus eventBus;

    protected EntrySequence entrySequence;

    protected StateMachine stateMachine = new EmptyStateMachine();

    protected int commitIndex = 0;

    AbstractLog(EventBus eventBus) {
        this.eventBus = eventBus;
    }


    private void applyEntry(Entry entry) {
        // skip no-op entry and membership-change entry
        if (isApplicable(entry)) {
            stateMachine.applyLog(entry.getIndex(), entry.getCommandBytes(), entrySequence.getFirstLogIndex());
        }
    }

    private boolean isApplicable(Entry entry) {
        return entry.getKind() == Entry.Kind_GENERAL;
    }
    //获取最后一条日志信息
    public EntryMeta getLastEntryMeta() {
        if (entrySequence.isEmpty()) {
            return new EntryMeta(Entry.Kind_NO_OP, 0, 0);
        }
        return entrySequence.getLastEntry().getMeta();
    }

    //创建AppendEntries
    public AppendEntriesRpc createAppendEntriesRpc(int term, NodeId selfId, int nextIndex, int maxEntries) {
        int nextLogIndex = entrySequence.getNextLogIndex();
        if (nextIndex > nextLogIndex) {
            throw new IllegalArgumentException("illegal next index " + nextIndex);
        }
        AppendEntriesRpc rpc = new AppendEntriesRpc();
        rpc.setMessageId(UUID.randomUUID().toString());
        rpc.setTerm(term);
        rpc.setLeaderId(selfId);
        rpc.setLeaderCommit(commitIndex);
        // 设置前一条日志的元信息，有可能不存在
        Entry entry  = entrySequence.getEntry(nextIndex-1);
        if (entry != null){
            rpc.setPrevLogTerm(entry.getTerm());
            rpc.setPrevLogIndex(entry.getIndex());
        }
        // 设置最大读取的日志条数，比如如果传输全部日志条目，那么会造成网络拥堵
        if (!entrySequence.isEmpty()) {
            int maxIndex = (maxEntries == ALL_ENTRIES ? nextLogIndex : Math.min(nextLogIndex, nextIndex + maxEntries));
            rpc.setEntries(entrySequence.subList(nextIndex, maxIndex));
        }
        return rpc;
    }

    //与前一条日志比较
    public boolean isNewerThan(int lastLogIndex, int lastLogTerm) {
        EntryMeta lastEntryMeta = getLastEntryMeta();
        logger.debug("last entry ({}, {}), candidate ({}, {})", lastEntryMeta.getIndex(), lastEntryMeta.getTerm(), lastLogIndex, lastLogTerm);
        return lastEntryMeta.getTerm() > lastLogTerm || lastEntryMeta.getIndex() > lastLogIndex;
    }

    //追加NO-OP日志
    public NoOpEntry appendEntry(int term) {
        NoOpEntry entry = new NoOpEntry(entrySequence.getNextLogIndex(), term);
        entrySequence.append(entry);
        return entry;
    }

    //追加一般日志
    public GeneralEntry appendEntry(int term, byte[] command) {
        //创建一个一般日志(末尾)
        GeneralEntry entry = new GeneralEntry(entrySequence.getNextLogIndex(), term, command);

        //追加日志
        entrySequence.append(entry);
        System.out.println("abstract log entry get command "+ Arrays.toString(entry.getCommandBytes()));

        //应用状态机
        stateMachine.applyLog(entry.getIndex(),entry.getCommandBytes(),0);
        System.out.println("abstract stateMachine apply log 完成");
        return entry;
    }

    //核心方法,follower追加leader发来的日志(解决日志冲突问题)
    public boolean appendEntriesFromLeader(int prevLogIndex, int prevLogTerm, List<Entry> leaderEntries) {

        //检查前一条日志是否匹配
        //先检查从leader节点过来的prevLogIndex和prevLogTerm是否匹配本地日志，如果不匹配则返回false，则追加失败
//        if (!checkIfPreviousLogMatches(prevLogIndex, prevLogTerm)) {
//            return false;
//        }

        //Leader节点传递过来的日志条目为空
        if (leaderEntries.isEmpty()) {
            return true;
        }

        // 移除冲突的日志条目并返回接下来要追加的日志条目
        assert prevLogIndex + 1 == leaderEntries.get(0).getIndex();
        //移除冲突的日志条目并返回接下来要追加的日志条目(如果还有的话)
        EntrySequenceView newEntries = removeUnmatchedLog(new EntrySequenceView(leaderEntries));

        //仅追加日志
        appendEntriesFromLeader(newEntries);
        return true;
    }

    //检查前一条日志是否匹配
    private boolean checkIfPreviousLogMatches(int prevLogIndex, int prevLogTerm) {

        //检查指定索引的日志条目
        EntryMeta meta = entrySequence.getEntryMeta(prevLogIndex);

        //日志不存在
        if (meta == null) {
            logger.debug("previous log {} not found ", prevLogIndex);
        }

        int term = meta.getTerm();
        if (term != prevLogTerm) {
            logger.debug("different term of previous log, local {}, remote {}", term, prevLogTerm);
            return false;
        }
        return true;
    }

    //移除冲突的日志条目并返回接下来要追加的日志条目(如果还有的话)
    private EntrySequenceView removeUnmatchedLog(EntrySequenceView leaderEntries) {

        //Leader节点过来的entries不应该为空
        assert !leaderEntries.isEmpty();

        //找到第一个不匹配的日志索引
        int firstUnmatched = findFirstUnmatchedLog(leaderEntries);

        //没有不匹配的日志
        if (firstUnmatched < 0) {
            return new EntrySequenceView(Collections.emptyList());
        }

        //移除不匹配的日志索引开始的所有日志
        removeEntriesAfter(firstUnmatched - 1);

        //返回之后追加的日志条目
        return leaderEntries.subView(firstUnmatched);

    }

    //找到第一个不匹配的日志索引
    private int findFirstUnmatchedLog(EntrySequenceView leaderEntries) {
        int logIndex;
        EntryMeta followerEntryMeta;

        //从前往后遍历leaderEntries
        for (Entry leaderEntry : leaderEntries) {
            logIndex = leaderEntry.getIndex();

            //按照索引查找日志条目元信息
            followerEntryMeta = entrySequence.getEntryMeta(logIndex);

            //日志不存在或者term不一致
            if (followerEntryMeta == null || followerEntryMeta.getTerm() != leaderEntry.getTerm()) {
                return logIndex;
            }
        }
        //否则没有不一致的日志条目
        return -1;
    }

    //移除不匹配的日志索引开始的所有日志
    private void removeEntriesAfter(int index) {
        if (entrySequence.isEmpty() || index >= entrySequence.getLastLogIndex()) {
            return;
        }

        entrySequence.subList(entrySequence.getFirstLogIndex(), index + 1).forEach(this::applyEntry);
        //此处如果移除了已经应用的日志,需要从头开始重新构建状态机
        logger.debug("remove entries after {}", index);
        entrySequence.removeAfter(index);
    }

    //追加来自leader的可用日志
    private void appendEntriesFromLeader(EntrySequenceView leaderEntries) {
        if (leaderEntries.isEmpty()) {
            return;
        }
        logger.debug("append entries from leader from {} to {}", leaderEntries.getFirstLogIndex(), leaderEntries.getLastLogIndex());
        for (Entry leaderEntry : leaderEntries) {

            //追加来自leader的可用日志(不同存储不同实现)
            entrySequence.append(leaderEntry);
        }
    }

    //推进一条的日志索引
    public void advanceCommitIndex(int newCommitIndex,int currentTerm){

        if (validateNewCommitIndex(newCommitIndex,currentTerm)){
            return;
        }
        logger.debug("advance commit index from {} to {}", commitIndex, newCommitIndex);
        entrySequence.commit(newCommitIndex);
        commitIndex = newCommitIndex;
    }

    //检查新的commitIndex
    private boolean validateNewCommitIndex(int newCommitIndex,int currentTerm){

        //小于当前的commitIndex
        if (newCommitIndex <= entrySequence.getCommitIndex()){
            return true;
        }
        EntryMeta meta = entrySequence.getEntryMeta(newCommitIndex);
        if (meta==null){
            logger.debug("log of new commit index {} not found",newCommitIndex);
            return false;
        }

        //日志条目的term必须是当前term,才可推进commitIndex
        if (meta.getTerm()!=currentTerm){
            logger.debug("log term of new commit index != current term ({}!={})",meta.getTerm(),currentTerm);
        }
        return false;
    }

    public void setStateMachine(StateMachine stateMachine) {
        this.stateMachine = stateMachine;
    }

    @Override
    public byte[] getLogByKey(String key) {
        if (key==null){
            throw new LogException("key is null");
        }
        return entrySequence.getLogByKey(key);
    }

    @Override
    public int getNextIndex() {
        return entrySequence.getNextLogIndex();
    }

    @Override
    public int getCommitIndex() {
        return entrySequence.getCommitIndex();
    }

    @Override
    public void close() {
        entrySequence.close();
    }

    //操作leader传来的日志条目数组
    private static class EntrySequenceView implements Iterable<Entry> {

        private final List<Entry> entries;

        private int firstLogIndex = -1;
        private int lastLogIndex = -1;

        //构造器
        private EntrySequenceView(List<Entry> entries) {
            this.entries = entries;
            if (!entries.isEmpty()) {
                firstLogIndex = entries.get(0).getIndex();
                lastLogIndex = entries.get(entries.size() - 1).getIndex();

            }
        }

        //获取指定位置的日志条目
        Entry get(int index) {
            if (entries.isEmpty() || index < firstLogIndex || index > lastLogIndex) {
                return null;
            }
            return entries.get(index - firstLogIndex);
        }

        boolean isEmpty() {
            return entries.isEmpty();
        }

        //获取第一条记录的索引,此处没有非空校验
        int getFirstLogIndex() {
            return firstLogIndex;
        }

        //获取最后一条记录的索引,此处没有非空校验
        int getLastLogIndex() {
            return lastLogIndex;
        }

        //获取子视图
        EntrySequenceView subView(int fromIndex) {
            if (entries.isEmpty() || fromIndex > lastLogIndex) {
                return new EntrySequenceView(Collections.emptyList());
            }
            return new EntrySequenceView(

                    //截取子数组(可用日志)
                    entries.subList(fromIndex - firstLogIndex, entries.size())
            );
        }

        @Override
        public Iterator<Entry> iterator() {
            return entries.iterator();
        }
    }


}


