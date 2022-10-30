package raft.core.node.role;

import com.google.common.base.Preconditions;
import raft.core.node.base.NodeId;

/**
 * 角色名和LeaderID
 * @author yiyewei
 * @create 2022/10/9 10:59
 **/
public class RoleNameAndLeaderId {

    private final RoleName roleName;
    private final NodeId leaderId;

    public RoleNameAndLeaderId(RoleName roleName, NodeId leaderId) {
        Preconditions.checkNotNull(roleName);
        this.roleName = roleName;
        this.leaderId = leaderId;
    }

    public RoleName getRoleName() {
        return roleName;
    }

    public NodeId getLeaderId() {
        return leaderId;
    }

}
