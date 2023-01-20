package raft.core.node.base;


import com.google.common.base.Preconditions;
import raft.core.rpc.Address;

/**
 * 节点服务期id,ip和端口类
 * @author yiyewei
 * @create 2022/9/19 20:57
 **/
public class NodeEndpoint {
    private final  NodeId id;
    private final Address address;




    //节点ID,ip,端口构造函数
    public NodeEndpoint(String  id,String host,int port) {
        this(new NodeId(id),new Address(host,port));
    }

    //节点ID，地址构造函数
    public NodeEndpoint(NodeId id, Address address) {
        Preconditions.checkNotNull(id);
        Preconditions.checkNotNull(address);
        this.id=id;
        this.address=address;
    }

    public Address getAddress() {
        return address;
    }

    public NodeId getId() {
        return id;
    }

    public int getPort() {
        return this.address.getPort();
    }
}


