package raft.kvstore.client;


import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.completer.ArgumentCompleter;
import org.jline.reader.impl.completer.NullCompleter;
import org.jline.reader.impl.completer.StringsCompleter;
import raft.core.node.base.NodeId;
import raft.core.rpc.Address;
import raft.kvstore.client.command.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class Console {

    private static final String PROMPT = "kvstore-client " + Client.VERSION + "> ";
    private final Map<String, Command> commandMap;
    private final CommandContext commandContext;
    private final LineReader reader;
    {
        commandMap = buildCommandMap(Arrays.asList(
                new ExitCommand(),
                new ClientAddServerCommand(),
                new ClientRemoveServerCommand(),
                new ClientListServerCommand(),
                new ClientGetLeaderCommand(),
                new ClientSetLeaderCommand(),
                new RaftAddNodeCommand(),
                new RaftRemoveNodeCommand(),
                new KVStoreGetCommand(),
                new KVStoreSetCommand()
        ));
        ArgumentCompleter completer = new ArgumentCompleter(
                new StringsCompleter(commandMap.keySet()),
                new NullCompleter()
        );
        reader = LineReaderBuilder.builder()
                .completer(completer)
                .build();
    }

    public Console(Map<NodeId, Address> serverMap) {
        commandContext = new CommandContext(serverMap);
    }

    private Map<String, Command> buildCommandMap(Collection<Command> commands) {
        Map<String, Command> commandMap = new HashMap<>();
        for (Command cmd : commands) {
            commandMap.put(cmd.getName(), cmd);
        }
        return commandMap;
    }

    void start() {
        commandContext.setRunning(true);
        showInfo();
        String line;
        while (commandContext.isRunning()) {
            try {
                line = reader.readLine(PROMPT);
                if (line.trim().isEmpty()){
                    continue;
                }
                //接收到行命令，分解
                dispatchCommand(line);
            } catch (IllegalArgumentException e) {
                System.err.println(e.getMessage());
            } catch (EndOfFileException ignored) {
                break;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void showInfo() {
        System.out.println("Welcome to XRaft KVStore Shell\n");
        System.out.println("***********************************************");
        System.out.println("current server list: \n");
        commandContext.printSeverList();
        System.out.println("***********************************************");
    }

    //差分命令
    private void dispatchCommand(String line) {
        String[] commandNameAndArguments = line.split("\\s+", 2);
        String commandName = commandNameAndArguments[0];
        Command command = commandMap.get(commandName);
        if (command == null) {
            throw new IllegalArgumentException("no such command [" + commandName + "]");
        }
        //执行
        command.execute(commandNameAndArguments.length > 1 ? commandNameAndArguments[1] : "", commandContext);
    }
}
