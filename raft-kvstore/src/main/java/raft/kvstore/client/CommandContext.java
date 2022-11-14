package raft.kvstore.client;

import raft.core.node.base.NodeId;
import raft.core.rpc.Address;
import raft.core.service.ServerRouter;

import java.util.Map;

/**
 * 命令上下文字段
 *
 * @author yiyewei
 * @create 2022/10/11 20:44
 **/
public class CommandContext {
    // 服务器列表
    private final Map<NodeId, Address> serverMap;
    // 客户端
    private Client client;
    // 是否在运行
    private boolean running = false;

    public CommandContext(Map<NodeId, Address> serverMap) {
        this.serverMap = serverMap;
        this.client = new Client(buildServerRouter(serverMap));
    }

    // 构建服务路由器
    private ServerRouter buildServerRouter(Map<NodeId, Address> serverMap) {
        ServerRouter router = new ServerRouter();
        for (NodeId nodeId : serverMap.keySet()) {
            Address address = serverMap.get(nodeId);
            router.add(nodeId, new SocketChannel(address.getHost(), address.getPort()));
        }
        return router;
    }

    public Client getClient() {
        return client;
    }

    public NodeId getClientLeader() {
        return client.getServerRouter().getLeaderId();
    }

    public void setClientLeader(NodeId nodeId) {
        client.getServerRouter().setLeaderId(nodeId);
    }

    public void clientAddServer(String nodeId, String host, int portService) {
        serverMap.put(new NodeId(nodeId), new Address(host, portService));
        client = new Client(buildServerRouter(serverMap));
    }

    public boolean clientRemoveServer(String nodeId) {
        Address address = serverMap.remove(new NodeId(nodeId));
        if (address != null) {
            client = new Client(buildServerRouter(serverMap));
            return true;
        }
        return false;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    boolean isRunning() {
        return running;
    }

    public void printSeverList() {
        for (NodeId nodeId : serverMap.keySet()) {
            Address address = serverMap.get(nodeId);
            System.out.println(nodeId + "," + address.getHost() + "," + address.getPort());
        }
    }

}
