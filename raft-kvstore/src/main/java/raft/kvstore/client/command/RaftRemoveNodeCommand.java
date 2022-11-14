package raft.kvstore.client.command;

import raft.core.service.exception.NoAvailableServerException;
import raft.kvstore.client.CommandContext;

public class RaftRemoveNodeCommand implements Command {

    @Override
    public String getName() {
        return "raft-remove-node";
    }

    @Override
    public void execute(String arguments, CommandContext context) {
        if (arguments.isEmpty()) {
            throw new IllegalArgumentException("usage " + getName() + " <node-id>");
        }

        try {
//            context.getClient().removeNode(arguments);
        } catch (NoAvailableServerException e) {
            System.err.println(e.getMessage());
        }
    }

}
