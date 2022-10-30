package raft.core.rpc;

/**
 * ip地址及端口号类
 * @author yiyewei
 * @create 2022/9/20 9:53
 **/
public class Address{
    private final String host;
    private final int port;

    public Address(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }
}