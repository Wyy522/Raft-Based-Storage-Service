package raft.kvstore.message;

import java.util.Arrays;

/**
 * 获取Key对应Value的返回值
 *
 * @author yiyewei
 * @create 2022/10/9 9:50
 **/
public class GetCommandResponse {
    private final boolean found;
    private final byte[] value;

    public GetCommandResponse(byte[] value) {
        this(value != null, value);
    }

    public GetCommandResponse(boolean found, byte[] value) {
        this.found = found;
        this.value = value;
    }

    public boolean isFound() {
        return found;
    }

    public byte[] getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "GetCommandResponse{" +
                "found=" + found;
    }
}
