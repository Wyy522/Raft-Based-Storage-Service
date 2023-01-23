package raft.core.node;

import com.google.common.base.Preconditions;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.FutureCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import raft.core.log.entry.EntryMeta;
import raft.core.log.execption.LogException;
import raft.core.log.execption.NotLeaderException;
import raft.core.log.statemachine.StateMachine;
import raft.core.node.base.GroupMember;
import raft.core.node.base.NodeEndpoint;
import raft.core.node.base.NodeId;
import raft.core.node.role.*;
import raft.core.node.store.NodeStore;
import raft.core.rpc.message.*;
import raft.core.scheduler.ElectionTimeout;
import raft.core.scheduler.LogReplicationTask;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

/**
 * leader选举具体逻辑实现
 *
 * @author yiyewei
 * @create 2022/9/21 20:22
 **/

public class NodeImpl implements Node {

    //日志
    private static final Logger logger = LoggerFactory.getLogger(NodeImpl.class);

    //核心组件
    private final NodeContext context;

    //是否已启动
    private boolean started;

    //当前的角色
    private AbstractNodeRole role;

    //构造函数
    public NodeImpl(NodeContext context) {
        this.context = context;
    }

    //Node启动
    @Override
    public synchronized void start() {
        //如果已经启动则直接跳过
        if (started) {
            return;
        }

        //注册自己到EventBus
        context.eventBus().register(this);

        //初始化连接器
        context.connector().initialize();

        //启动时为Follower角色
        NodeStore store = context.store();

        //统一角色转换(节点初次启动设置Follower相关参数)
        changeToRole(new FollowerNodeRole(store.getTerm(), store.getVotedFor(), null, scheduleElectionTimeout()));

        //设置节点状态为启动
        started = true;
    }

    //统一角色变化,以及在角色变化时同步到NodeStore中
    private void changeToRole(AbstractNodeRole newRole) {
        //打印日志
        logger.debug("node {},role state changed ->{}", context.getSelfId(), newRole);

        //获取上下文环境中节点的状态信息
        NodeStore store = context.store();

        //将上下文环境中节点状态信息中的任期号持久化
        store.setTerm(newRole.getTerm());

        //如果是Follower,则将上下文环境中的投票信息持久化
        if (newRole.getName() == RoleName.FOLLOWER) {
            store.setVotedFor(((FollowerNodeRole) newRole).getVotedFor());
        }

        role = newRole;

    }

    @Override
    public void stop() throws InterruptedException {
        if (!started) {
            throw new IllegalStateException("node not started");
        }

        //关闭定时器
        context.scheduler().stop();

        //关闭连接器
        context.connector().close();

        //关闭执行器
        context.taskExecutor().shutdown();

        //设置节点状态为关闭
        started = false;

    }

    @Override
    public void registerStateMachine(StateMachine stateMachine) {
        Preconditions.checkNotNull(stateMachine);
        System.out.println("StateMachine 已注入");
        context.log().setStateMachine(stateMachine);
    }

    @Override
    public void appendLog(byte[] commandBytes) {
        Preconditions.checkNotNull(commandBytes);
        ensureLeader();
        context.taskExecutor().submit(() -> {
            //leader 进行日志存储
            context.log().appendEntry(role.getTerm(), commandBytes);
            //leader转发给其他节点进行日志存储
            doReplicateLog();

        }, LOGGING_FUTURE_CALLBACK);
    }

    private void ensureLeader() {
        RoleNameAndLeaderId result = role.getNameAndLeaderId(context.selfId());
        if (result.getRoleName() == RoleName.LEADER) {
            return;
        }
        NodeEndpoint endpoint = result.getLeaderId() != null ? context.group().findMember(result.getLeaderId()).getEndpoint() : null;
        throw new NotLeaderException(result.getRoleName(), endpoint);
    }

    @Override
    public RoleNameAndLeaderId getRoleNameAndLeaderId() {
        return role.getNameAndLeaderId(context.selfId());
    }


    //特殊的角色切换方法,有一个是否设置选举超时参数
    private void becomeFollower(int term, NodeId votedFor, NodeId leaderId, boolean scheduleElectionTimeout) {

        //取消定时任务
        role.cancelTimeoutTask();

        if (leaderId != null && leaderId.equals(role.getLeaderId(context.selfId()))) {
            logger.info("current leader is {}, term {}", leaderId, term);
        }

        //重新创建选举超时定时器或空定时器
        ElectionTimeout electionTimeout = scheduleElectionTimeout ? scheduleElectionTimeout() : ElectionTimeout.NONE;

        changeToRole(new FollowerNodeRole(term, votedFor, leaderId, electionTimeout));
    }

    //————————————————————————————————————————————选举———————————————————————————————————————————————————————————————————

    //设置一个选举超时定时任务(当超时后由TaskExecutor的实现类异步调用选举的具体逻辑)  到时间后在定时线程(异步单线程)中执行
    private ElectionTimeout scheduleElectionTimeout() {
        return context.scheduler().scheduleElectionTimeout(this::electionTimout);
    }

    //提交选举超时任务(超时后调用此逻辑)  定时线程到时后在处理逻辑线程中(异步单线程)调用此逻辑
    public void electionTimout() {
        context.taskExecutor().submit(this::doProcessElectionTimeout);
    }

    //(选举一)选举实现的具体逻辑
    private void doProcessElectionTimeout() {

        //第一个超时的follower会首先调用该方法,执行选举流程
        //1.给自己的任期号+1,票数+1(自己给自己投的)
        //2.切换角色为candidate
        //3.构造投票请求并向除自己以外的其他节点发送该请求

        //leader角色不可能有选举超时
        if (role.getName() == RoleName.LEADER) {
            logger.warn("node {}，current role is leader, ignore election timeout", context.selfId());
            return;
        }

        //当前节点任期号+1
        int newTerm = role.getTerm() + 1;

        //每次切换角色都需要取消当前的定时任务
        role.cancelTimeoutTask();
        logger.info("start election");

        //变成Candidate角色,给自己投上一票,并再次开始选举超市任务
        changeToRole(new CandidateNodeRole(newTerm, scheduleElectionTimeout()));

        //获取最后一条日志的元信息
        EntryMeta lastEntryMeta = context.getLog().getLastEntryMeta();

        //封装投票请求Rpc
        RequestVoteRpc rpc = new RequestVoteRpc();
        rpc.setTerm(newTerm);
        rpc.setCandidateId(context.selfId());
        rpc.setLastLogIndex(lastEntryMeta.getIndex());
        rpc.setLastLogTerm(lastEntryMeta.getTerm());

        //Candidate群发投票请求RPC,征集投票
        context.connector().sendRequestVote(rpc, context.getGroup().listEndPointExceptSelf());
    }

    @Subscribe
    public void onReceiveRequestVoteRpc(RequestVoteRpcMessage rpcMessage) {
        context.taskExecutor().submit(
                () -> {
                    RequestVoteResult result = doProcessRequestVoteRpc(rpcMessage);
                    if (result != null) {
                        context.connector().replyRequestVote(result, rpcMessage);
                    }
                },
                LOGGING_FUTURE_CALLBACK
        );
    }

    //(选举二)除leader外的节点处理投票请求的具体逻辑
    private RequestVoteResult doProcessRequestVoteRpc(RequestVoteRpcMessage rpcMessage) {

        RequestVoteRpc rpc = rpcMessage.get();
        /** 基本流程
         1.首先判断对方的term决定是否投票
         (1)比自己小：不投票且返回自己的term(说明当前自己就是leader);
         (2)比自己大：投票且切换自己为Follower更新自己的term;
         (3)与自己等：
         ①当前自己是Follower:比较日志进度,比自己的日志进度新且自己还未投票,则投票更新自己的term;
         若自己以投票给该节点,则投票更新自己的term;
         比自己日志进度旧无论投没投票,不投票且返回自己的term;
         ②当前自己是Candidate:不投票,因为切换角色时以给自己投过票;
         ③当前自己是Leader:不投票且返回自己的term
         2.构造响应candidate的Rpc
         如果对方的term比自己小,则不投票且返回自己的term给对象(说明自己就是leader)
        **/
        if (rpc.getTerm() < role.getTerm()) {
            logger.debug("term from rpc < current term, don't vote ({} < {})", rpc.getTerm(), role.getTerm());
            return new RequestVoteResult(role.getTerm(), false);
        }

        //如果对象的term比自己大,则切换为Follower角色
        if (rpc.getTerm() > role.getTerm()) {
            //比较自己最后一条日志与消息中最后一条日志(isNewerThan返回结果为true时是我新，再取反为false就是不投票)
            boolean voteForCandidate = !context.getLog().isNewerThan(rpc.getLastLogIndex(), rpc.getLastLogTerm());
            becomeFollower(rpc.getTerm(), (voteForCandidate ? rpc.getCandidateId() : null), null, true);
            return new RequestVoteResult(rpc.getTerm(), voteForCandidate);
        }

        // 本地term与消息term一致
        assert rpc.getTerm() == role.getTerm();
        //当term相同
        switch (role.getName()) {
            case FOLLOWER:
                FollowerNodeRole follower = (FollowerNodeRole) role;
                NodeId votedFor = follower.getVotedFor();
                //两种情况,1是自己尚未投票且对方的日志比自己新,2是自己已经给对方投过票
                if ((votedFor == null && !context.log().isNewerThan(rpc.getLastLogIndex(), rpc.getLastLogTerm()))
                        || Objects.equals(votedFor, rpc.getCandidateId())) {
                    //投票后需要切换为Follower角色
                    becomeFollower(role.getTerm(), rpc.getCandidateId(), null, true);
                    return new RequestVoteResult(rpc.getTerm(), true);
                }
                return new RequestVoteResult(role.getTerm(), false);
            case CANDIDATE://已经给自己投过票,所以不会给其他节点投票
            case LEADER:
                return new RequestVoteResult(role.getTerm(), false);
            default:
                throw new IllegalStateException("unexpected node role[" + role.getName() + "]");
        }
    }

    //candidate监听到有RequestVoteResult消息后(由TaskExecutor的实现类异步处理投票请求响应的结果)
    @Subscribe
    public void onReceiveRequestVoteResult(RequestVoteResult result) {
        context.taskExecutor().submit(
                () -> {
                    doProcessRequestVoteResult(result);
                }
        );
    }

    //(选举三)candidate分析此次投票请求结果RequestVoteResult并进行下一步操作的具体逻辑
    private void doProcessRequestVoteResult(RequestVoteResult result) {
        //1.如果对方的term比自己大,说明集群中已存在leader,则将自己切换为Follower;
        //2.如果对方的term比自己小或者对方没有给自己投票,则忽略(因为不可能存在比自己小的term,至少是相等的);
        //3.统计得票数,得票数超过集群节点的一半则切换自己的角色为Leader,否则继续成为Candidate重复选举流程

        //如果对方的term比自己大,则将自己退化为Follower
        if (result.getTerm() > role.getTerm()) {
            becomeFollower(result.getTerm(), null, null, true);
            return;
        }

        //如果自己不是Candidate角色,则忽略
        if (role.getName() != RoleName.CANDIDATE) {
            logger.debug("receive request vote result and current role is not Candidate, ignore");
            return;
        }

        //如果对方的term比自己小或者对方没有给自己投票,则忽略
        if (!result.isVoteGranted()) {
            return;
        }

        //当前票数
        int currentVotesCount = ((CandidateNodeRole) role).getVotesCount() + 1;

        //节点数
        int countOfMajor = context.group().getCount();
        logger.debug("votes count {}, major node count {}", currentVotesCount, countOfMajor);

        //取消选举超时器
        role.cancelTimeoutTask();

        //当票数过半
        if (currentVotesCount > countOfMajor / 2) {
            logger.info("become leader, term {}", role.getTerm());
            // 重置选举进度
            resetReplicatingStates();
            changeToRole(new LeaderNodeRole(role.getTerm(), scheduleLogReplicationTask()));
            // 猜测添加一个no-op是迅速告诉其他节点的leader节点已经更换
            context.log().appendEntry(role.getTerm()); // no-op log
        } else {
            //修改收到的投票数,并重新创建选举超时定时器
            changeToRole(new CandidateNodeRole(role.getTerm(), currentVotesCount, scheduleElectionTimeout()));
        }
    }

    //——————————————————————————————————————————日志复制——————————————————————————————————————————————————————————————————

    //设置一个日志复制定时任务(也可称之为心跳,每隔logReplicationInterval时间后由taskExecutor的实现类异步执行日志复制的具体逻辑)
    private LogReplicationTask scheduleLogReplicationTask() {
        return context.scheduler().scheduleLogReplicationTask(this::replicateLog);
    }

    //提交一个日志复制任务(每次间隔时间到后调用此逻辑)
    public void replicateLog() {
        context.taskExecutor().submit(this::doReplicateLog);
    }

    //(日志复制一)日志复制任务的具体逻辑(由leader发起,封装日志复制请求给集群中除自己以外的节点发送)
    private void doReplicateLog() {
        logger.debug("replicate log");

        //给日志复制对象节点发送AppendEntries消息(即出自己以外的节点)
        for (GroupMember member : context.group().listReplicationTarget()) {
            doReplicateLogFin(member, -1);
        }
    }

    private void doReplicateLogFin(GroupMember member, int maxEntries) {
        AppendEntriesRpc rpc = context.log().createAppendEntriesRpc(role.getTerm(), context.selfId(), member.getNextIndex(), maxEntries);
        context.connector().sendAppendEntries(rpc, member.getEndpoint());

    }

    //除leader外的节点监听到有AppendEntries消息后(由TaskExecutor的实现类异步构造一个日志复制请求的响应)
    @Subscribe
    public void onReceiveAppendEntriesRpc(AppendEntriesRpcMessage rpcMessage) {
        context.taskExecutor().submit(() ->
                        context.connector().replyAppendEntries(doProcessAppendEntriesRpc(rpcMessage), rpcMessage),
                LOGGING_FUTURE_CALLBACK
        );
    }

    //(日志复制二)日志复制的具体流程
    private AppendEntriesResult doProcessAppendEntriesRpc(AppendEntriesRpcMessage rpcMessage) {

        //1.首先判断对方的term决定是否进行日志复制
        //  (1)比自己小：不复制且返回自己的term(说明当前自己就是leader);
        //  (2)比自己大：复制且切换自己为Follower更新自己的term;
        //  (3)与自己等：
        //          ①当前自己是Follower:复制,设置leaderId为该节点Id并重置选举定时器;
        //          ②当前自己是Candidate:复制，当前节点退化为Follower并重置选举定时器;
        //          ③当前自己是Leader:不复制,忽略;
        //2.构造响应leader的Rpc

        AppendEntriesRpc rpc = rpcMessage.get();
        //如果对方term比自己小直接返回自己的term
        if (rpc.getTerm() < role.getTerm()) {
            return new AppendEntriesResult(rpc.getMessageId(), role.getTerm(), false);
        }

        //如果对方的term比自己大,则转化角色为Follower
        if (rpc.getTerm() > role.getTerm()) {
            becomeFollower(rpc.getTerm(), null, rpc.getLeaderId(), true);

            //并追加日志
            return new AppendEntriesResult(rpc.getMessageId(), rpc.getTerm(), appendEntries(rpc));
        }

        assert rpc.getTerm() == role.getTerm();

        switch (role.getName()) {
            case FOLLOWER:
                //设置leaderId并重置选举定时器
                becomeFollower(rpc.getTerm(), ((FollowerNodeRole) role).getVotedFor(), rpc.getLeaderId(), true);
//                this.context.log()
                //追加日志
                return new AppendEntriesResult(rpc.getMessageId(), rpc.getTerm(), appendEntries(rpc));
            case CANDIDATE:

                //如果有两个Candidate角色,并且另一个Candidate先成了leader,则当前节点退化为Follower,重置选举定时器
                becomeFollower(rpc.getTerm(), null, rpc.getLeaderId(), true);

                //追加日志
                return new AppendEntriesResult(rpc.getMessageId(), rpc.getTerm(), appendEntries(rpc));
            case LEADER:

                //Leader角色收到AppendEntries消息,忽略并打日志
                logger.warn("receive append entries rpc from another leader {}, ignore", rpc.getLeaderId());
                return new AppendEntriesResult(rpc.getMessageId(), rpc.getTerm(), false);
            default:
                throw new IllegalStateException("unexpected node role[" + role.getName() + "]");
        }

    }

    //follower追加日志的具体逻辑
    private boolean appendEntries(AppendEntriesRpc rpc) {

        System.out.println("1.日至正在同步");
        boolean result = context.getLog().appendEntriesFromLeader(rpc.getPrevLogIndex(), rpc.getPrevLogTerm(), rpc.getEntries());
        if (result) {
            context.getLog().advanceCommitIndex(
                    Math.max(rpc.getLeaderCommit(), rpc.getLastEntryIndex()), rpc.getTerm()
            );
            System.out.println("2.日至已同步");
        }
        return result;
    }


    //leader监听到有AppendEntriesResult的消息后(由TaskExecutor的实现类异步处理日志复制响应的结果)
    @Subscribe
    public void onReceiveAppendEntriesResult(AppendEntriesResultMessage resultMessage) {
        context.taskExecutor().submit(() -> doProcessAppendEntriesResult(resultMessage), LOGGING_FUTURE_CALLBACK);
    }

    //(日志复制三)leader分析此次日志复制结果AppendEntriesResult并进行下一步操作的具体逻辑
    private void doProcessAppendEntriesResult(AppendEntriesResultMessage resultMessage) {

        //1.如果对方的term比自己大,说明集群中已存在leader,则将自己切换为Follower;
        //2.检查自己的角色
        //3.leader日志提交
        //

        AppendEntriesResult result = resultMessage.get();

        // 如果对方的term比自己大，则退化为follower角色
        if (result.getTerm() > role.getTerm()) {
            becomeFollower(result.getTerm(), null, null, true);
            return;
        }

        //检查自己的角色
        if (role.getName() != RoleName.LEADER) {
            logger.warn("receive append result from node {} but current node is not leader, ignore", resultMessage.getSourceNodeId());
        }

        //对于自身是leader情况的情况下 推进commitIndex
        NodeId sourceNodeId = resultMessage.getSourceNodeId();
        GroupMember member = context.getGroup().getMember(sourceNodeId);
        //没有指定成员
        if (member == null) {
            logger.info("unexpected append entries result from node {}, node maybe removed", sourceNodeId);
            return;
        }

        AppendEntriesRpc rpc = resultMessage.getRpc();
        if (result.isSuccess()) {
            // 回复成功,推进matchIndex和nextIndex
            // 在收到成功响应的分支中，member的advanceReplicatingState负责把节点的matchIndex更新为消息中的最后一条日志的索引，nextIndex更新为matchIndex+1
            // 如果matchIndex和nextIndex没有变化，则advanceReplicatingState返回false
            if (member.advanceReplicatingState(rpc.getLastEntryIndex())) {
                // getMatchIndexOfMajor是服务器成员用于计算过半commitIndex的方法。
                context.getLog().advanceCommitIndex(
                        context.group().getMatchIndexOfMajor(), role.getTerm()
                );
                System.out.println("3.日志同步成功");
            }
        } else {
            if (member.backOffNextIndex()) {
                logger.warn("cannot back off next index more , node {}", sourceNodeId);
            }
        }
    }

    private void resetReplicatingStates() {
        context.group().resetReplicatingStates(context.log().getNextIndex());
    }

    //包可见,获取核心组件上下文
    public NodeContext getContext() {
        return this.context;
    }

    //包可见,获取当前角色
    public AbstractNodeRole getRole() {
        return this.role;
    }


    private static final FutureCallback<Object> LOGGING_FUTURE_CALLBACK = new FutureCallback<Object>() {
        @Override
        public void onSuccess(@Nullable Object result) {
        }

        @Override
        public void onFailure(@Nonnull Throwable t) {
            logger.warn("failure", t);
        }
    };

    public byte[] getLogByKey(String key) {
        if (key == null) {
            throw new LogException("key is null");
        }
        return context.log().getLogByKey(key);
    }
}
