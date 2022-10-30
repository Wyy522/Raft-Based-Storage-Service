package raft.core.node.base;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 集群成员类
 * @author yiyewei
 * @create 2022/9/19 19:54
 **/

public class NodeGroup {


    private static final Logger logger = LoggerFactory.getLogger(NodeGroup.class);

    //当前节点ID
    private final NodeId selfId;

    //成员表
    private Map<NodeId, GroupMember> memberMap;

    public NodeId getSelfId() {
        return selfId;
    }

    public Map<NodeId, GroupMember> getMemberMap() {
        return memberMap;
    }

    public void setMemberMap(Map<NodeId, GroupMember> memberMap) {
        this.memberMap = memberMap;
    }

    //单节点构造函数
    public NodeGroup(NodeEndpoint endpoint) {
        //返回一个只含该对象的不可变set集合
        this(Collections.singleton(endpoint), endpoint.getId());
    }

    //多节点构造函数
    public NodeGroup(Collection<NodeEndpoint> endpoints, NodeId selfId) {
        this.memberMap = buildMemberMap(endpoints);
        this.selfId = selfId;
    }

    //返回当前节点数
    public int getCount(){
        return memberMap.size();
    }


    //从节点列表中构造成员映射表
    private Map<NodeId, GroupMember> buildMemberMap(Collection<NodeEndpoint> endpoints) {
        Map<NodeId, GroupMember> map = new HashMap<>();

        //构造
        for (NodeEndpoint endpoint : endpoints) {
            map.put(endpoint.getId(), new GroupMember(endpoint));
        }

        //不许成员表为空
        if (map.isEmpty()) {
            throw new IllegalArgumentException("endpoints is empty");
        }

        return map;
    }

    //按照节点Id查找成员，找不到时抛错
    public GroupMember findMember(NodeId id) {
        GroupMember member = getMember(id);
        if (member == null) {
            throw new IllegalArgumentException("no such node" + id);
        }
        return member;
    }

    //按照节点Id查找成员，找不到时返回空
    public GroupMember getMember(NodeId id) {
        return memberMap.get(id);
    }

    //列出日志复制的对象节点，即除自己以外的所有节点
    public Collection<GroupMember> listReplicationTarget() {
        return memberMap.values().stream().filter(m -> !m.idEquals(selfId)).collect(Collectors.toList());
    }

    //返回当前节点之外的其他节点
    public Set<NodeEndpoint> listEndPointExceptSelf() {
        Set<NodeEndpoint> endpoints = new HashSet<>();
        for (GroupMember member : memberMap.values()) {
            //判断是不是当前节点
            if (!member.getId().equals(selfId)) {
                endpoints.add(member.getEndpoint());
            }
        }
        return endpoints;
    }

    //除leader外
    public int getMatchIndexOfMajor() {
        List<NodeMatchIndex> matchIndices = new ArrayList<>();
        for (GroupMember member : memberMap.values()) {
            if (!member.idEquals(selfId)) {
                matchIndices.add(new NodeMatchIndex(member.getId(), member.getMatchIndex()));
            }
        }
        int count = matchIndices.size();
        // 没有节点的情况
        if (count == 0) {
            throw new IllegalStateException("no major node");
        }
//        if (count == 1 && matchIndices.get(0).nodeId == selfId) {
//            throw new IllegalStateException("standalone");
//        }
        Collections.sort(matchIndices);
        logger.debug("match indices {}", matchIndices);
        // 取排序后的中间位置的matchIndex
        int index = (count % 2 == 0 ? count / 2 - 1 : count / 2);
        return matchIndices.get(index).getMatchIndex();
    }

    public void resetReplicatingStates(int nextLogIndex) {
        for (GroupMember member : memberMap.values()) {
            if (!member.idEquals(selfId)) {
                member.setReplicatingState(new ReplicatingState(nextLogIndex));
            }
        }
    }

    public GroupMember findSelf() {
        return findMember(selfId);
    }

    /**
 * 记录节点 id 和 matchIndex
 * @author yiyewei
 * @create 2022/9/28 22:54
 **/
    private static class NodeMatchIndex implements Comparable<NodeMatchIndex> {

        private final NodeId nodeId;
        private final int matchIndex;
        private final boolean leader;

        NodeMatchIndex(NodeId nodeId) {
            this(nodeId, Integer.MAX_VALUE, true);
        }

        NodeMatchIndex(NodeId nodeId, int matchIndex) {
            this(nodeId, matchIndex, false);
        }

        private NodeMatchIndex(NodeId nodeId, int matchIndex, boolean leader) {
            this.nodeId = nodeId;
            this.matchIndex = matchIndex;
            this.leader = leader;
        }

        int getMatchIndex() {
            return matchIndex;
        }

        @Override
        public int compareTo(@Nonnull NodeMatchIndex o) {
            return Integer.compare(this.matchIndex, o.matchIndex);
        }

        @Override
        public String toString() {
            return "<" + nodeId + ", " + (leader ? "L" : matchIndex) + ">";
        }

    }

}
