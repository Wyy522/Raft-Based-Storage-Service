package raft.core.log.entry;

/**
 * 抽象实现日志条目
 * @author yiyewei
 * @create 2022/9/26 10:44
 **/
abstract class AbstractEntry implements Entry{

    //日志条目类型
    private final int kind;

    //日志索引
    protected final int index;

    //当前日志term
    protected final int term;

    public AbstractEntry(int kind, int index, int term) {
        this.kind = kind;
        this.index = index;
        this.term = term;
    }

    @Override
    public int getKind() {
        return kind;
    }

    @Override
    public int getIndex() {
        return index;
    }

    @Override
    public int getTerm() {
        return term;
    }

    //获取元数据
    @Override
    public EntryMeta getMeta() {
        return new EntryMeta(kind,index,term);
    }
}
