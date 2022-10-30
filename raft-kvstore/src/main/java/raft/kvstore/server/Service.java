package raft.kvstore.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import raft.core.log.statemachine.AbstractSingleThreadStateMachine;
import raft.core.node.Node;
import raft.core.node.NodeImpl;
import raft.core.node.role.RoleName;
import raft.core.node.role.RoleNameAndLeaderId;
import raft.kvstore.message.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Service {
    private static final Logger logger = LoggerFactory.getLogger(Service.class);

    private final Node node;

    //请求ID和CommandRequest的映射类
    private final ConcurrentHashMap<String, CommandRequest<?>> pendingCommands = new ConcurrentHashMap<>();

    private  Map<String, byte[]> map = new HashMap<>();

    public Service(Node node) {
        this.node = node;
        this.node.registerStateMachine(new StateMachineImpl());
    }

    public void set(CommandRequest<SetCommand> commandRequest){
        // 如果当前节点不是leader节点那么就返回redirect

        //重定向
        Redirect redirect = checkLeadership();
        if (redirect != null){
            commandRequest.reply(redirect);
        }
        SetCommand command = commandRequest.getCommand();
//        String value =new String(command.getValue());
        logger.debug("set key :{} , value : {}",command.getKey(),command.getValue());
        // 记录请求ID和CommandRequest的映射
        this.pendingCommands.put(command.getRequestId(),commandRequest);
        // 客户端连接关闭时从映射中移除
        commandRequest.addCloseListener(()->pendingCommands.remove(command.getRequestId()));
        // 追加日志
        this.node.appendLog(command.toBytes());

        //返回客户端
        commandRequest.reply(new SetCommand(command.getKey(), command.getValue()));

    }

    public void get(CommandRequest<GetCommand> commandRequest) {
//        Redirect redirect = checkLeadership();
//        if (redirect != null) {
//            logger.info("reply {}", redirect);
//            commandRequest.reply(redirect);
//            return;
//        }
//        GetCommand command = commandRequest.getCommand();
//        logger.debug("get {}", command.getKey());
//
//        byte[] value = this.map.get(command.getKey());
//        System.out.println("get value :"+ Arrays.toString(value));
//        commandRequest.reply(new GetCommandResponse(value));

        String key = commandRequest.getCommand().getKey();
        logger.debug("get {}", key);
        //TODO how to find key in memory log?
//        byte[] bytes = map.get(key);
//        System.out.println("get value : "+Arrays.toString(bytes));
//        commandRequest.reply(new GetCommandResponse(bytes));
        byte[] value = this.node.getLogByKey(key);
        System.out.println("get value : "+Arrays.toString(value));
        commandRequest.reply(new GetCommandResponse(value));
    }

    //检查是否是Leader
    private Redirect checkLeadership(){
        RoleNameAndLeaderId state = node.getRoleNameAndLeaderId();
        if (state.getRoleName() != RoleName.LEADER){
            return new Redirect(state.getLeaderId());
        }
        return null;
    }


    private class StateMachineImpl extends AbstractSingleThreadStateMachine {

        @Override
        //应用命令
        protected void applyCommand(byte[] commandBytes) {
            SetCommand command = SetCommand.fromBytes(commandBytes);
            map.put(command.getKey(), command.getValue());
            System.out.println("StateMachine map value :"+ Arrays.toString(map.get(command.getKey())));
            CommandRequest<?> commandRequest = pendingCommands.remove(command.getRequestId());
            if (commandRequest != null) {
                commandRequest.reply(Success.INSTANCE);
            }
        }
    }

}
