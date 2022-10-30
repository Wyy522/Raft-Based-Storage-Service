package raft.kvstore.message;

import raft.core.node.base.NodeId;

/**
 * 重定向到Leader节点
 *
 * @author yiyewei
 * @create 2022/10/9 9:49
 **/
public class Redirect {

    private final String leaderId;

    public Redirect(NodeId leaderId) {
        this(leaderId != null ? leaderId.getValue() : null);
    }

    public Redirect(String leaderId) {
        this.leaderId = leaderId;
    }

    public String getLeaderId() {
        return leaderId;
    }

    @Override
    public String toString() {
        return "RedirectId{" +
                "leaderId='" + leaderId + '\'' +
                '}';
    }
}
