package raft.core.node.base;

import com.google.common.base.Preconditions;

import java.io.Serializable;
import java.util.Objects;

/**
 * 服务器节点ID类
 * @author yiyewei
 * @create 2022/9/19 16:13
 **/
public class NodeId implements Serializable {

    private final String value;

    //构造节点ID
    public static NodeId of(String value) {
        return new NodeId(value);
    }

    public NodeId(String value) {
        //判空
        Preconditions.checkNotNull(value);
        this.value = value;
    }

    public String getValue() {
        return value;

    }

    @Override
    public boolean equals(Object o) {
        if (this == o){
            return true;
        }
        if (!(o instanceof NodeId))
        {
            return false;
        }
        NodeId id = (NodeId) o;
        return Objects.equals(value, id.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return this.value;
    }

}
