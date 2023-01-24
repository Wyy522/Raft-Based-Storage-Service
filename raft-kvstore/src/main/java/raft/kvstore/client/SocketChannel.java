package raft.kvstore.client;

import com.google.protobuf.ByteString;
import com.google.protobuf.MessageLite;
import raft.core.node.base.NodeId;
import raft.core.service.Channel;
import raft.core.service.exception.ChannelException;
import raft.core.service.exception.RedirectException;
import raft.kvstore.MessageConstants;
import raft.kvstore.Protos;
import raft.kvstore.message.GetCommand;
import raft.kvstore.message.SetCommand;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;

public class SocketChannel implements Channel {

    private final String host;
    private final int port;

    public SocketChannel(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    public Object send(Object payload) {
        try (Socket socket = new Socket()) {
            socket.setTcpNoDelay(true);
            //kv client 连接 kv server   localhost,3333
            System.out.println("socket channel host :"+this.host+" , port : "+this.port);
            //bio连接服务器
            socket.connect(new InetSocketAddress(this.host, this.port));
            //发送数据
            this.write(socket.getOutputStream(), payload);
            //BIO会在这里阻塞 直到获取到消息
            return this.read(socket.getInputStream());
        } catch (IOException e) {
            throw new ChannelException("failed to send and receive", e);
        }
    }

    private void write(OutputStream output, Object payload) throws IOException {
        if (payload instanceof GetCommand) {
            Protos.GetCommand protoGetCommand = Protos.GetCommand.newBuilder().setKey(((GetCommand) payload).getKey()).build();
            //为了解决tcp的半包粘包问题，在发送的数据前面加上消息类型和消息长度
            this.write(output, MessageConstants.MSG_TYPE_GET_COMMAND, protoGetCommand);
        } else if (payload instanceof SetCommand) {
            SetCommand setCommand = (SetCommand) payload;
            Protos.SetCommand protoSetCommand = Protos.SetCommand.newBuilder()
                    .setKey(setCommand.getKey())
                    .setValue(ByteString.copyFrom(setCommand.getValue())).build();
            this.write(output, MessageConstants.MSG_TYPE_SET_COMMAND, protoSetCommand);
        }
    }
    //为了解决tcp的半包粘包问题，在发送的数据前面加上消息类型和消息长度
    private void write(OutputStream output, int messageType, MessageLite message) throws IOException {
        DataOutputStream dataOutput = new DataOutputStream(output);
        byte[] messageBytes = message.toByteArray();
        dataOutput.writeInt(messageType);           //4字节 数据类型
        dataOutput.writeInt(messageBytes.length);   //4字节 数据长度
        dataOutput.write(messageBytes);             //不定长 数据内容
        dataOutput.flush();                         //清空发送
    }

    //读取到消息后按格式解析，并按规定的类型通过Protos工具转变换成对应的类
    private Object read(InputStream input) throws IOException {
        DataInputStream dataInput = new DataInputStream(input);
        int messageType = dataInput.readInt();
        int payloadLength = dataInput.readInt();
        byte[] payload = new byte[payloadLength];
        dataInput.readFully(payload);
        switch (messageType) {
            case MessageConstants.MSG_TYPE_SUCCESS:
                return null;
            case MessageConstants.MSG_TYPE_FAILURE:
                Protos.Failure protoFailure = Protos.Failure.parseFrom(payload);
                throw new ChannelException("error code " + protoFailure.getErrorCode() + ", message " + protoFailure.getMessage());
            case MessageConstants.MSG_TYPE_REDIRECT:
                Protos.Redirect protoRedirect = Protos.Redirect.parseFrom(payload);
                throw new RedirectException(new NodeId(protoRedirect.getLeaderId()));
            case MessageConstants.MSG_TYPE_GET_COMMAND_RESPONSE:
                Protos.GetCommandResponse protoGetCommandResponse = Protos.GetCommandResponse.parseFrom(payload);
                if (!protoGetCommandResponse.getFound()) {return null;}
                return protoGetCommandResponse.getValue().toByteArray();
            case MessageConstants.MSG_TYPE_SET_COMMAND:
                Protos.SetCommand protoSetCommand = Protos.SetCommand.parseFrom(payload);
                System.out.println("OK");
                return protoSetCommand.getValue().toByteArray();
            default:
                throw new ChannelException("unexpected message type " + messageType);
        }
    }




}
