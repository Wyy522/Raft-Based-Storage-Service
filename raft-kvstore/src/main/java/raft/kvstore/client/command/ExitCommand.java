package raft.kvstore.client.command;

import raft.kvstore.client.CommandContext;
import raft.kvstore.client.command.Command;

public class ExitCommand implements Command {

    @Override
    public String getName() {
        return "exit";
    }

    @Override
    public void execute(String arguments, CommandContext context) {
        System.out.println("bye");
        context.setRunning(false);
    }

}
