package raft.kvstore.message;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import raft.kvstore.Protos;

import java.util.Arrays;
import java.util.UUID;
/**
 * Set
 * @author yiyewei
 * @create 2022/10/9 9:53
 **/
public class SetCommand {
    private final String requestId;
    private final String key;
    private final byte[] value;

    //请求无ID
    public SetCommand(String key, byte[] value) {
        this(UUID.randomUUID().toString(),key,value);
    }

    public SetCommand(String requestId, String key, byte[] value) {
        this.requestId = requestId;
        this.key = key;
        this.value = value;
    }

    //从二进制数组中恢复SetCommand
    public static SetCommand fromBytes(byte[] bytes) {
        try {
            Protos.SetCommand protoCommand = Protos.SetCommand.parseFrom(bytes);
            return new SetCommand(
                    protoCommand.getRequestId(),
                    protoCommand.getKey(),
                    protoCommand.getValue().toByteArray()
            );
        } catch (InvalidProtocolBufferException e) {
            throw new IllegalStateException("failed to deserialize set command", e);
        }
    }

    //转换为二进制数组
    public byte[] toBytes() {
        return Protos.SetCommand.newBuilder()
                .setRequestId(this.requestId)
                .setKey(this.key)
                .setValue(ByteString.copyFrom(this.value)).build().toByteArray();
    }


    public String getRequestId() {
        return requestId;
    }

    public String getKey() {
        return key;
    }

    public byte[] getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "SetCommand{" +
                "requestId='" + requestId + '\'' +
                ", key='" + key + '\'' +
                ", value=" + Arrays.toString(value) +
                '}';
    }
}
