package raft.core.log.sequence;

import raft.core.log.entry.Entry;
import raft.core.log.entry.EntryMeta;
import raft.core.log.execption.EmptySequenceException;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 抽象实现日志条目序列
 *
 * @author yiyewei
 * @create 2022/9/26 9:34
 **/
public abstract class AbstractEntrySequence implements EntrySequence {

    //日志索引偏移量
    public int logIndexOffset;

    //下一条日志的索引号
    public int nextLogIndex;

    //构造器,默认为空日志条目序列
    public AbstractEntrySequence(int logIndexOffset) {
        this.logIndexOffset = logIndexOffset;
        this.nextLogIndex = logIndexOffset;
    }

    //判断是否为空
    public boolean isEmpty() {
        return logIndexOffset == nextLogIndex;
    }

    //获取第一条日志的索引,为空的话抛错
    public int getFirstLogIndex() {
        if (isEmpty()) {
            throw new EmptySequenceException();
        }
        return doGetFirstLogIndex();
    }

    public int doGetFirstLogIndex() {
        return logIndexOffset;
    }

    //获取最后一条日志的索引,为空的话抛错
    public int getLastLogIndex() {
        if (isEmpty()) {
            throw new EmptySequenceException();
        }
        return doGetLastLogIndex();
    }

   public int doGetLastLogIndex() {
        return nextLogIndex - 1;
    }

    //获取下一条日志的索引
    public int getNextLogIndex() {
        return nextLogIndex;
    }

    //判断日志条目是否存在
    public boolean isEntryPresent(int index) {
        return !isEmpty()
                && index >= doGetFirstLogIndex()
                && index <= doGetLastLogIndex();
    }

    //获取指定索引日志条目的元信息
    public EntryMeta getEntryMeta(int index) {
        Entry entry = getEntry(index);
        return entry != null ? entry.getMeta() : null;
    }

    //获取指定索引的日志条目
    public Entry getEntry(int index) {
        if (!isEntryPresent(index)) {
            return null;
        }
        return doGetEntry(index);
    }

    public Entry getLastEntry() {
        if (!isEntryPresent(doGetLastLogIndex())) {
            return null;
        }
        return doGetEntry(doGetLastLogIndex());
    }

    //获取指定索引的日志条目(有不同的存储方式,交给子类实现)
    protected abstract Entry doGetEntry(int index);

    //获取序列的子视图，到最后一条日志
    public List<Entry> subList(int fromIndex) {
        if (isEmpty() || fromIndex > doGetLastLogIndex()) {
            return Collections.emptyList();
        }
        return subList(Math.max(fromIndex, doGetFirstLogIndex()), nextLogIndex);
    }

    //获取序列的子视图，到toIndex的闭区间
    public List<Entry> subList(int fromIndex, int toIndex) {
        if (isEmpty()) {
            throw new EmptySequenceException();
        }

        //检查索引
        if (fromIndex < doGetFirstLogIndex()
                || toIndex > getLastLogIndex() + 1
                || fromIndex > toIndex) {
            throw new IllegalArgumentException("illegal from index " + fromIndex + " or to index " + toIndex);
        }
        return doSubList(fromIndex, toIndex);
    }

    //获取序列的子视图，到toIndex的闭区间(有不同的存储方式,交给子类实现)
    protected abstract List<Entry> doSubList(int fromIndex, int toIndex);

    //追加单条日志
    public void append(Entry entry) {
        //保证新日志的索引是当前序列的下一条日志索引
        if (entry.getIndex() != nextLogIndex) {
            throw new IllegalArgumentException("entry index must be " + nextLogIndex);
        }
        doAppend(entry);

        //递增序列的日志索引
        nextLogIndex++;
    }

    //追加多条日志
    public void append(List<Entry> entries) {
        for (Entry entry : entries) {
            append(entry);
        }
    }

    //具体添加日志由子类实现
    protected abstract void doAppend(Entry entry);

    //移出指定索引后的日志条目
    public void removeAfter(int index) {
        if (isEmpty() || index >= getLastLogIndex()) {
            return;
        }
        doRemoveAfter(index);
    }

    //移出指定索引后的日志条目(有不同的存储方式,交给子类实现)
    protected abstract void doRemoveAfter(int index);

    public void commit(int index) {

    }

    public int getCommitIndex() {
        return 0;
    }


    public void close() {

    }
}
