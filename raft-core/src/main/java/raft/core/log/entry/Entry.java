package raft.core.log.entry;

/**
 * 日志条目
 * @author yiyewei
 * @create 2022/9/24 23:03
 **/
public interface Entry {

    //日志类型
    int Kind_NO_OP=0;
    int Kind_GENERAL=1;

    //获取类型
    int getKind();

    //获取索引
    int getIndex();

    //获取term
    int getTerm();

    //获取元信息(kind,term,index)
    EntryMeta getMeta();

    //获取日志负载
    byte[] getCommandBytes();

}
