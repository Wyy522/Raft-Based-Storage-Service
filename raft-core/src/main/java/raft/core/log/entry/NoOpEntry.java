package raft.core.log.entry;

/**
 * 新Leader节点增加的第一条空日志(目的是为了提交上一任期的日志，防止覆盖),没有负载信息,不需要在上层服务的状态机中应用
 * @author yiyewei
 * @create 2022/9/24 23:18
 **/

public class NoOpEntry extends AbstractEntry{

    public NoOpEntry(int index, int term) {
        super(Kind_NO_OP, index, term);
    }

    @Override
    public byte[] getCommandBytes() {
        return new byte[0];
    }
}
