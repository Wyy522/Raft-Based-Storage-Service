package raft.kvstore.client;
/**
 * KV数据库操作 添加数据
 * @author yiyewei
 * @create 2022/10/11 22:18
 **/
public class KVStoreSetCommand implements Command {

    @Override
    public String getName() {
        return "kvstore-set";
    }

    @Override
    public void execute(String arguments, CommandContext context) {
        int index = arguments.indexOf(' ');
        if (index <= 0 || index == arguments.length() - 1) {
            throw new IllegalArgumentException("usage: " + getName() + " <key> <value>");
        }
        context.getClient().set(arguments.substring(0, index), arguments.substring(index + 1).getBytes());
    }

}
