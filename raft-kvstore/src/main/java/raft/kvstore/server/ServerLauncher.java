package raft.kvstore.server;


import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import raft.core.node.Node;
import raft.core.node.NodeBuilder;
import raft.core.node.base.NodeEndpoint;
import raft.core.node.base.NodeId;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// TODO load config from file
public class ServerLauncher {

    private static final Logger logger = LoggerFactory.getLogger(ServerLauncher.class);
    private static final String MODE_STANDALONE = "standalone";
    private static final String MODE_STANDBY = "standby";
    private static final String MODE_GROUP_MEMBER = "group-member";

    // TODO why volatile?
    private volatile Server server;

    private void execute(String[] args) throws Exception {
        Options options = new Options();
        //模式
        {
        options.addOption(Option.builder("m")
                .hasArg()
                .argName("mode")
                .desc("start mode, available: standalone, standby, group-member. default is standalone")
                .build());
        //节点ID
        options.addOption(Option.builder("i")
                .longOpt("id")
                .hasArg()
                .argName("node-id")
                .required()
                .desc("node id, required. must be unique in group. " +
                        "if starts with mode group-member, please ensure id in group config")
                .build());
        //主机名称
        options.addOption(Option.builder("h")
                .hasArg()
                .argName("host")
                .desc("host, required when starts with standalone or standby mode")
                .build());
        //Raft服务端口
        options.addOption(Option.builder("p1")
                .longOpt("port-raft-node")
                .hasArg()
                .argName("port")
                .type(Number.class)
                .desc("port of raft node, required when starts with standalone or standby mode")
                .build());
        //KV服务端口
        options.addOption(Option.builder("p2")
                .longOpt("port-service")
                .hasArg()
                .argName("port")
                .type(Number.class)
                .required()
                .desc("port of service, required")
                .build());
        //日志目录
        options.addOption(Option.builder("d")
                .hasArg()
                .argName("data-dir")
                .desc("data directory, optional. must be present")
                .build());
        //志群配置
        options.addOption(Option.builder("gc")
                .hasArgs()
                .argName("node-endpoint")
                .desc("group config, required when starts with group-member mode. format: <node-endpoint> <node-endpoint>..., " +
                        "format of node-endpoint: <node-id>,<host>,<port-raft-node>, eg: A,localhost,8000 B,localhost,8010")
                .build());
        }

        if (args.length == 0) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("raft-kvstore [OPTION]...", options);
            return;
        }
        //解析命令行参数
        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cmdLine = parser.parse(options, args);
            //group-member
            String mode = cmdLine.getOptionValue('m', MODE_STANDALONE);
            //集群启动
            if (MODE_GROUP_MEMBER.equals(mode)) {
                startAsGroupMember(cmdLine);
            } else {
                throw new IllegalArgumentException("illegal mode [" + mode + "]");
            }
        } catch (ParseException | IllegalArgumentException e) {
            System.err.println(e.getMessage());
        }
    }

    private void startAsGroupMember(CommandLine cmdLine) throws Exception {
        if (!cmdLine.hasOption("gc")) {
            throw new IllegalArgumentException("group-config required");
        }

        String rawNodeId = cmdLine.getOptionValue('i');//服务节点名称 A
        int portService = ((Long) cmdLine.getParsedOptionValue("p2")).intValue();  //对外服务端口 3333

        String[] rawGroupConfig = cmdLine.getOptionValues("gc");//解析集群成员 A,localhost,2333 B,localhost,2334 C,localhost,2335
        Set<NodeEndpoint> nodeEndpoints = Stream.of(rawGroupConfig)
                .map(this::parseNodeEndpoint)
                .collect(Collectors.toSet());

        //解析完成后 创建core节点相关信息(连接器、日志、选举等等)
        Node node = new NodeBuilder(nodeEndpoints, new NodeId(rawNodeId))
                .setDataDir(cmdLine.getOptionValue('d'))
                .build();//第一个节点内部Netty服务器初始化等操作
        //创建对外Server服务
        Server server = new Server(node, portService);
        logger.info("start as group member, group config {}, id {}, port service {}", nodeEndpoints, rawNodeId, portService);
        startServer(server);
    }

    private NodeEndpoint parseNodeEndpoint(String rawNodeEndpoint) {
        String[] pieces = rawNodeEndpoint.split(",");
        if (pieces.length != 3) {
            throw new IllegalArgumentException("illegal node endpoint [" + rawNodeEndpoint + "]");
        }
        String nodeId = pieces[0];
        String host = pieces[1];
        int port;
        try {
            port = Integer.parseInt(pieces[2]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("illegal port in node endpoint [" + rawNodeEndpoint + "]");
        }
        return new NodeEndpoint(nodeId, host, port);
    }

    private void startServer(Server server) throws Exception {
        this.server = server;
        this.server.start();
        Runtime.getRuntime().addShutdownHook(new Thread(this::stopServer, "shutdown"));
    }

    private void stopServer() {
        try {
            server.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        ServerLauncher launcher = new ServerLauncher();
        launcher.execute(args);
    }

}

//          switch (mode) {
//                case MODE_STANDBY:
//                    startAsStandaloneOrStandby(cmdLine, true);
//                    break;
//                case MODE_STANDALONE:
//                    startAsStandaloneOrStandby(cmdLine, false);
//                    break;
//                case MODE_GROUP_MEMBER:
//                    //集群启动
//                    startAsGroupMember(cmdLine);
//                    break;
//                default:
//                    throw new IllegalArgumentException("illegal mode [" + mode + "]");
//            }
//    private void startAsStandaloneOrStandby(CommandLine cmdLine, boolean standby) throws Exception {
//        if (!cmdLine.hasOption("p1") || !cmdLine.hasOption("p2")) {
//            throw new IllegalArgumentException("port-raft-node or port-service required");
//        }
//
//        String id = cmdLine.getOptionValue('i');
//        String host = cmdLine.getOptionValue('h', "localhost");
//        int portRaftServer = ((Long) cmdLine.getParsedOptionValue("p1")).intValue();
//        int portService = ((Long) cmdLine.getParsedOptionValue("p2")).intValue();
//
//        NodeEndpoint nodeEndpoint = new NodeEndpoint(id, host, portRaftServer);
//        Node node = new NodeBuilder(nodeEndpoint)
//                .setStandby(standby)
//                .setDataDir(cmdLine.getOptionValue('d'))
//                .build();
//        Server server = new Server(node, portService);
//        logger.info("start with mode {}, id {}, host {}, port raft node {}, port service {}",
//                (standby ? "standby" : "standalone"), id, host, portRaftServer, portService);
//        startServer(server);
//    }
