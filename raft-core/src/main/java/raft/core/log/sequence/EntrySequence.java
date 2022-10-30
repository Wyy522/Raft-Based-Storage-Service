package raft.core.log.sequence;

import raft.core.log.entry.Entry;
import raft.core.log.entry.EntryMeta;

import java.util.List;

/**
 * 日志条目序列接口
 * @author yiyewei
 * @create 2022/9/26 9:19
 **/
public interface EntrySequence {

    //判断该日志条目序列是否为空
    boolean isEmpty();

    //获取第一个日志的索引号
    int getFirstLogIndex();

    //获取最后一个日志的索引号
    int getLastLogIndex();

    //获取下一个日志的索引号
    int getNextLogIndex();

    //获取序列的子视图，到最后一条日志
    List<Entry> subList(int fromIndex);

    //获取序列的子视图，到toIndex的闭区间
    List<Entry> subList(int fromIndex, int toIndex);

    //检查某个日志条目是否存在
    boolean isEntryPresent(int index);

    //获取某个日志条目的元信息
    EntryMeta getEntryMeta(int index);

    //获取某个日志条目
    Entry getEntry(int index);

    //获取最后一个日志条目
    Entry getLastEntry();

    //追加日志条目
    void append(Entry entry);

    //追加多个日志条目
    void append(List<Entry> entries);

    //推荐commitIndex(已提交的索引)
    void commit(int index);

    //获取当前已提交的索引
    int getCommitIndex();

    //移出某个索引之后的日志条目
    void removeAfter(int index);

    //关闭日志序列
    void close();

    byte[] getLogByKey(String key);
}
