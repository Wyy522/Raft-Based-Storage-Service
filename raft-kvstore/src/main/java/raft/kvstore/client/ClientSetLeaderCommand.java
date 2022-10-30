package raft.kvstore.client;

import raft.core.node.base.NodeId;
/**
 * 设置Leader服务器
 * @author yiyewei
 * @create 2022/10/11 22:16
 **/
public class ClientSetLeaderCommand implements Command{
    @Override
    public String getName() {
        return "client-set-leader";
    }

    @Override
    public void execute(String arguments, CommandContext context) {
        //判断后续参数是否为空
        if (arguments.isEmpty()){
            throw new IllegalArgumentException("usage: "+getName()+"<node-id>");
        }

        //设置新的Leader节点Id
        NodeId nodeId =new NodeId(arguments);
        try {
            context.setClientLeader(nodeId);
            System.out.println(nodeId);
        }catch (IllegalStateException e){
            System.err.println(e.getMessage());
        }
    }
}
