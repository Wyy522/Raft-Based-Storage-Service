package raft.core.rpc;

import raft.core.rpc.message.AppendEntriesResult;
import raft.core.rpc.message.AppendEntriesRpc;
import raft.core.rpc.message.RequestVoteResult;
import raft.core.rpc.message.RequestVoteRpc;

import javax.annotation.Nonnull;

public interface Channel {

    void writeRequestVoteRpc(RequestVoteRpc rpc);

    void writeRequestVoteResult(RequestVoteResult result);

    void writeAppendEntriesRpc(AppendEntriesRpc rpc);

    void writeAppendEntriesResult(AppendEntriesResult result);

    void close();
}
