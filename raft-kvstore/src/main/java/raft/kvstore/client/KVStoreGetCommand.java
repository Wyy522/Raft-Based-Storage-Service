package raft.kvstore.client;

import raft.core.service.NoAvailableServerException;
/**
 * KV数据库操作 获取数据
 * @author yiyewei
 * @create 2022/10/11 22:17
 **/
public class KVStoreGetCommand implements Command {

    @Override
    public String getName() {
        return "kvstore-get";
    }

    @Override
    public void execute(String arguments, CommandContext context) {
        if (arguments.isEmpty()) {
            throw new IllegalArgumentException("usage: " + getName() + " <key>");
        }

        byte[] valueBytes;
        try {
            valueBytes = context.getClient().get(arguments);
        } catch (NoAvailableServerException e) {
            System.err.println(e.getMessage());
            return;
        }

        if (valueBytes == null) {
            System.out.println("null");
        } else {
            System.out.println(new String(valueBytes));
        }
    }

}
