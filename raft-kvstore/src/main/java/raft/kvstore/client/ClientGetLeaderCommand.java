package raft.kvstore.client;

/**
 * 显示Leader服务器
 * @author yiyewei
 * @create 2022/10/11 22:16
 **/
public class ClientGetLeaderCommand implements Command {

    @Override
    public String getName() {
        return "client-get-leader";
    }

    @Override
    public void execute(String arguments, CommandContext context) {
        System.out.println(context.getClientLeader());
    }

}
