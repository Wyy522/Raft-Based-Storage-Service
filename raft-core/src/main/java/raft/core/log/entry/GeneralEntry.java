package raft.core.log.entry;

/**
 * 上层服务产生的日志
 * @author yiyewei
 * @create 2022/9/24 23:19
 **/
public class GeneralEntry extends AbstractEntry{

    //日志负载(上层服务操作的内容)
    private final byte[] commandBytes;

    public GeneralEntry(int index, int term, byte[] commandBytes) {
        super(Kind_GENERAL, index, term);
        this.commandBytes = commandBytes;
    }

    @Override
    public byte[] getCommandBytes() {
        return commandBytes;
    }

    @Override
    public String toString() {
        return "GeneralEntry{" +
                "index=" + index +
                ", term=" + term +
                '}';
    }
}
