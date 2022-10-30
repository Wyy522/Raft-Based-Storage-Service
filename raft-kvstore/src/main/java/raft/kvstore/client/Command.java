package raft.kvstore.client;
/**
 * 命令的统一接口
 * @author yiyewei
 * @create 2022/10/11 20:44
 **/
public interface Command {
    //获取命令名
    String getName();
    //执行
    void execute(String arguments,CommandContext context);
}
