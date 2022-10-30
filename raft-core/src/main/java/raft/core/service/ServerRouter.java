package raft.core.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import raft.core.node.base.NodeId;

import java.util.*;


/**
 * 客户端内部负责处理KV服务端的重定向和选择Leader节点的路由器类
 * @author yiyewei
 * @create 2022/10/11 21:12
 **/

//ServerRouter选择Leader服务器原则：
//1：如果没有节点，则抛出没有服务节点的错误
//2：如果有手动设置的leader节点id，则返回手动设置的leader节点id与其他leader节点id的列表
//3：如果没有手动设置的leader节点id，那么则返回任意的节点id列表

public class ServerRouter {
    private static final Logger logger = LoggerFactory.getLogger(ServerRouter.class);

    private final Map<NodeId, Channel> availableServers =new HashMap<>();

    private NodeId leaderId;

    //发送消息
    public Object send(Object payload){

        //遍历服务路由表内所有节点(在某个节点返回未响应时，发送到其他节点)
        for (NodeId nodeId : getCandidateNodeIds()) {
            try {
                Object result = doSend(nodeId, payload);
                this.leaderId = nodeId;
                return result;
            } catch (RedirectException e) {
                // 收到重定向请求，修改leader节点id
                logger.debug("not a leader server, redirect to server {}", e.getLeaderId());
                this.leaderId = e.getLeaderId();
                return doSend(e.getLeaderId(), payload);
            } catch (Exception e) {
                // 连接失败，尝试下一个节点
                logger.debug("failed to process with server " + nodeId + ", cause " + e.getMessage());
            }
        }
        throw new NoAvailableServerException("no available server");
    }

    //获取候选节点id列表
    private Collection<NodeId> getCandidateNodeIds(){
        //候选为空
        if (availableServers.isEmpty()){
            throw  new NoAvailableServerException("no available server");
        }

        //已设置
        if (leaderId!=null){
            List<NodeId> nodeIds = new ArrayList<>();
            //把已设置的LeaderId放在候选的最前面
            nodeIds.add(leaderId);
            for (NodeId nodeId :availableServers.keySet()) {
                if (!nodeId.equals(leaderId)){
                    nodeIds.add(nodeId);
                }
            }
            return nodeIds;
        }

        //没有设置的话,任意返回
        return availableServers.keySet();
    }

    private Object doSend(NodeId id, Object payload) {
        Channel channel = this.availableServers.get(id);
        if (channel == null) {
            throw new IllegalStateException("no such channel to server " + id);
        }
//        logger.debug("send request to server {}", id);
        System.out.println("log: 该请求已发送给节点 :"+id);

        //        if (send ==null) {
//            throw new IllegalStateException("send to node :" + id+",receive is null ");
//        }
//        System.out.println("log: 当前请求类型为 :"+type);
//        if (type.equals("set")){
//            for (NodeId nodeId : availableServers.keySet()) {
//                if (nodeId==leaderId){
//                    continue;
//                }
//                Channel otherChannel = availableServers.get(nodeId);
//                otherChannel.send(payload);
//            }
//        }
        return channel.send(payload);
    }

    public void add(NodeId id, Channel channel) {
        this.availableServers.put(id, channel);
    }

    public void setLeaderId(NodeId leaderId) {
        if (!availableServers.containsKey(leaderId)) {
            throw new IllegalStateException("no such server [" + leaderId + "] in list");
        }
        this.leaderId = leaderId;
    }

    public NodeId getLeaderId() {
        return leaderId;
    }


}
