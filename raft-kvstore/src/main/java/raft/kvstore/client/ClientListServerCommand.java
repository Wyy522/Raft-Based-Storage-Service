package raft.kvstore.client;
/**
 * 显示集群服务器
 * @author yiyewei
 * @create 2022/10/11 22:14
 **/
public class ClientListServerCommand implements Command {

    @Override
    public String getName() {
        return "client-list-server";
    }

    @Override
    public void execute(String arguments, CommandContext context) {
        context.printSeverList();
    }

}
