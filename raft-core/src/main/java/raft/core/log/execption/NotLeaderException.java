package raft.core.log.execption;

import com.google.common.base.Preconditions;
import raft.core.node.base.NodeEndpoint;
import raft.core.node.role.RoleName;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Thrown when current node is not leader.
 */
public class NotLeaderException extends RuntimeException {

    private final RoleName roleName;
    private final NodeEndpoint leaderEndpoint;


    public NotLeaderException(@Nonnull RoleName roleName, @Nullable NodeEndpoint leaderEndpoint) {
        super("not leader");
        Preconditions.checkNotNull(roleName);
        this.roleName = roleName;
        this.leaderEndpoint = leaderEndpoint;
    }

    public RoleName getRoleName() {
        return roleName;
    }

    public NodeEndpoint getLeaderEndpoint() {
        return leaderEndpoint;
    }
    
}
