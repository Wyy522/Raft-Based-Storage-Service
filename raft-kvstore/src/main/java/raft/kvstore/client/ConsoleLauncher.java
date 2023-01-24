package raft.kvstore.client;

import org.apache.commons.cli.*;
import raft.core.node.base.NodeId;
import raft.core.node.base.Address;

import java.util.HashMap;
import java.util.Map;

public class ConsoleLauncher {

    private void execute(String[] args) {
        Options options = new Options();
        options.addOption(Option.builder("gc")
                .hasArgs()
                .argName("server-config")
                .required()
                .desc("group config, required. format: <server-config> <server-config>. " +
                        "format of server config: <node-id>,<host>,<port-service>. e.g A,localhost,8001 B,localhost,8011")
                .build());
        if (args.length == 0) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("raft-kvstore-client [OPTION]...", options);
            return;
        }

        CommandLineParser parser = new DefaultParser();
        Map<NodeId, Address> serverMap;
        try {
            CommandLine commandLine = parser.parse(options, args);
            //解析集群启动参数，创建服务器表
            serverMap = parseGroupConfig(commandLine.getOptionValues("gc"));
        } catch (ParseException | IllegalArgumentException e) {
            System.err.println(e.getMessage());
            return;
        }
        //封装client端的服务器表
        //构建core核心的路由表(nodeId,Channel)
        Console console = new Console(serverMap);
        //启动客户端
        console.start();
    }

    private Map<NodeId, Address> parseGroupConfig(String[] rawGroupConfig) {
        Map<NodeId, Address> serverMap = new HashMap<>();
        //A,localhost,3333 B,localhost,3334 C,localhost,3335
        for (String rawServerConfig : rawGroupConfig) {
            ServerConfig serverConfig = parseServerConfig(rawServerConfig);
            serverMap.put(new NodeId(serverConfig.getNodeId()), new Address(serverConfig.getHost(), serverConfig.getPort()));
        }
        return serverMap;
    }

    private ServerConfig parseServerConfig(String rawServerConfig) {
        //A,localhost,3333
        String[] pieces = rawServerConfig.split(",");
        if (pieces.length != 3) {
            throw new IllegalArgumentException("illegal server config [" + rawServerConfig + "]");
        }
        String nodeId = pieces[0]; //A
        String host = pieces[1];//localhost
        int port;
        try {
            port = Integer.parseInt(pieces[2]);//3333
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("illegal port [" + pieces[2] + "]");
        }
        return new ServerConfig(nodeId, host, port);
    }

    public static void main(String[] args) throws Exception {
        ConsoleLauncher launcher = new ConsoleLauncher();
        //入口 -gc A,localhost,3333 B,localhost,3334 C,localhost,3335
        launcher.execute(args);
    }

    private static class ServerConfig {

        private final String nodeId;
        private final String host;
        private final int port;

        ServerConfig(String nodeId, String host, int port) {
            this.nodeId = nodeId;
            this.host = host;
            this.port = port;
        }

        String getNodeId() {
            return nodeId;
        }

        String getHost() {
            return host;
        }

        int getPort() {
            return port;
        }

    }

}
