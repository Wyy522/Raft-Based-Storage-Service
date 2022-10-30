package raft.kvstore.client;

import raft.core.service.ServerRouter;
import raft.kvstore.message.GetCommand;
import raft.kvstore.message.SetCommand;

import java.util.Arrays;

public class Client {
    //版本
    public static final String VERSION = "0.1.0";
    public static final String SetType = "set";
    public static final String GetType = "get";
    //服务期路由器
    private final ServerRouter serverRouter;

    //构造函数
    public Client(ServerRouter serverRouter) {
        this.serverRouter = serverRouter;
    }

//    public void addNote(String nodeId, String host, int port) {
//        serverRouter.send(new AddNodeCommand(nodeId, host, port));
//    }
//
//    public void removeNode(String nodeId) {
//        serverRouter.send(new RemoveNodeCommand(nodeId));
//    }

    //KV服务SET命令
    public void set(String key, byte[] value) {
        serverRouter.send(new SetCommand(key, value));
    }

    //KV服务GET命令
    public byte[] get(String key) {
        return (byte[]) serverRouter.send(new GetCommand(key));
    }

    //获取内部服务器路由器
    public ServerRouter getServerRouter() {
        return serverRouter;
    }
}
