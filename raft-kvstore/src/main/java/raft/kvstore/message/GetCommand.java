package raft.kvstore.message;
/**
 * 获取Key对应的Value
 * @author yiyewei
 * @create 2022/10/9 9:50
 **/
public class GetCommand {
    private final String key;

    public GetCommand(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    @Override
    public String toString() {
        return "GetCommand{" +
                "key='" + key + '\'' +
                '}';
    }
}
