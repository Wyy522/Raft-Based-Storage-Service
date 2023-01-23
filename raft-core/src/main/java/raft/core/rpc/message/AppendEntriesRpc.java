package raft.core.rpc.message;

import raft.core.log.entry.Entry;
import raft.core.node.base.NodeId;

import java.util.Collections;
import java.util.List;

/**
 * 追加日志请求Rpc类(由leader发起)
 * @author yiyewei
 * @create 2022/9/20 15:33
 **/
public class AppendEntriesRpc {

    //自己当前的任期号
    private int term;

    //leader(自己)的Id
    private NodeId leaderId;

    //前一个日志索引号
    private  int prevLogIndex =0;

    //前一个日志的任期号
    private  int prevLogTerm;

    //复制的日志条目
    private List<Entry>entries = Collections.emptyList();

    //leader已提交的日志号
    private int leaderCommit;

    private String messageId;

    public int getLastEntryIndex() {
        return this.entries.isEmpty() ? this.prevLogIndex : this.entries.get(this.entries.size() - 1).getIndex();
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public int getTerm() {
        return term;
    }

    public void setTerm(int term) {
        this.term = term;
    }

    public NodeId getLeaderId() {
        return leaderId;
    }

    public void setLeaderId(NodeId leaderId) {
        this.leaderId = leaderId;
    }

    public int getPrevLogIndex() {
        return prevLogIndex;
    }

    public void setPrevLogIndex(int prevLogIndex) {
        this.prevLogIndex = prevLogIndex;
    }

    public int getPrevLogTerm() {
        return prevLogTerm;
    }

    public void setPrevLogTerm(int prevLogTerm) {
        this.prevLogTerm = prevLogTerm;
    }

    public List<Entry> getEntries() {
        return entries;
    }

    public void setEntries(List<Entry> entries) {
        this.entries = entries;
    }

    public int getLeaderCommit() {
        return leaderCommit;
    }

    public void setLeaderCommit(int leaderCommit) {
        this.leaderCommit = leaderCommit;
    }

    @Override
    public String toString() {
        return "AppendEntriesRpc{" +
                "term=" + term +
                ", leaderId=" + leaderId +
                ", prevLogIndex=" + prevLogIndex +
                ", prevLogTerm=" + prevLogTerm +
                ", entries=" + entries +
                ", leaderCommit=" + leaderCommit +
                '}';
    }

}
