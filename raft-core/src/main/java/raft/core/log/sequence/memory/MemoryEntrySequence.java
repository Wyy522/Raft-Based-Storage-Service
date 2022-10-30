package raft.core.log.sequence.memory;

import raft.core.log.command.SetCommand;
import raft.core.log.entry.Entry;
import raft.core.log.sequence.AbstractEntrySequence;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于内存的日志序列
 * @author yiyewei
 * @create 2022/9/26 10:41
 **/
public class MemoryEntrySequence extends AbstractEntrySequence {

    //存储日志条目的数据结构
    private final List<Entry> entries =new ArrayList<>();
    private final Map<String,byte[]> test=new ConcurrentHashMap<>();
    //已提交的日志索引号
    private int commitIndex=0;

    //构造器,日志索引偏移为1
    public MemoryEntrySequence() {
        super(1);
    }

    //构造器,指定日志索引偏移
    public MemoryEntrySequence(int logIndexOffset) {
        super(logIndexOffset);
    }

    @Override
    public byte[] getLogByKey(String key) {
        return test.get(key);
    }
    //按照索引获取指定条目
    @Override
    protected Entry doGetEntry(int index) {
       return entries.get(index-logIndexOffset);
    }

    //获取子视图
    @Override
    protected List<Entry> doSubList(int fromIndex, int toIndex) {
        return entries.subList(fromIndex-logIndexOffset,toIndex-logIndexOffset);
    }

    //追加日志条目
    @Override
    protected void doAppend(Entry entry) {
        SetCommand setCommand = SetCommand.fromBytes(entry.getCommandBytes());
        System.out.println("test map key :"+setCommand.getKey()+", value :"+ Arrays.toString(entry.getCommandBytes()));
        test.put(setCommand.getKey(),setCommand.getValue());
        entries.add(entry);

    }

    //提交索引
    @Override
    public void commit(int index) {
        commitIndex=index;
    }

    //获取提交索引
    @Override
    public int getCommitIndex() {
        return commitIndex;
    }

    //移出指定索引后的日志条目
    @Override
    protected void doRemoveAfter(int index) {
        if (index<doGetFirstLogIndex()){
            entries.clear();
            nextLogIndex=logIndexOffset;
        }else {
            entries.subList(index-logIndexOffset+1,entries.size()).clear();
            nextLogIndex=index+1;
        }
    }

    @Override
    public void close() {
    }


}
