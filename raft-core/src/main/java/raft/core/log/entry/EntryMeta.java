package raft.core.log.entry;

/**
 * 日志条目的元信息
 * @author yiyewei
 * @create 2022/9/26 10:43
 **/
public class EntryMeta {

    //日志条目类型
    private final int kind;

    //日志索引
    protected final int index;

    //当前日志term
    protected final int term;

    public EntryMeta(int kind, int index, int term) {
        this.kind = kind;
        this.index = index;
        this.term = term;
    }

    public int getKind() {
        return kind;
    }

    public int getIndex() {
        return index;
    }

    public int getTerm() {
        return term;
    }

}
