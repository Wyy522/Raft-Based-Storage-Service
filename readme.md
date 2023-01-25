# 基于Raft协议的分布式键值存储服务

## 一、简介

​         该项目参考《In Search of an Understandable Consensus Algorithm》论文实现Raft协议中三个子问题中的前两个，领导人选举(Leader election)和日志复制(Log replication)。使用网络编程框架Netty处理核心节点之间的RPC(RequestVote RPC和AppendEntries RPC)请求交互，核心节点同时也开启对外服务端口等待客户端连接发送命令。接收到来自客户端的Get、Set命令后Leader节点通过AppendEntries RPC保证集群内节点将命令中的数据以Lsm-tree的结构持久化落盘。

> 分布式共识算法那么多,为什么使用Raft ?
>
> 答：在学术理论界,最闪耀的当属的**Paxos**,但将其工程落地相当复杂,绝大多数人认为其难以理解,晦涩难懂(这一点在上述提到的论文中也有阐述),本人同样也是对其一知半解且资料也相对较少，故放弃Paxos。相比 **ZAB**,Raft 的设计更为简洁。以可理解性和易于实现为目标的Raft算法极大的帮助了我们对分布式共识算法的理解,也正是易于实现让我选择了Raft。
>
> 什么是分布式系统，分布式与集群的区别？
>
> 答：由于普通的单机服务器面对业务量巨大的场景下无法满足要求,垂直扩展升级机器硬件和水平扩展堆廉价服务器是两种最常见的解决方案,目前互联网领域绝大多数选择了后者水平扩展,也就是加机器。分布式(distributed): 在多台不同的服务器中部署不同的服务模块,通过[远程调用](https://so.csdn.net/so/search?q=远程调用&spm=1001.2101.3001.7020)协同工作,对外提供服务。集群(cluster): 在多台不同的服务器中部署相同的应用和服务模块,通过负载均衡设备对外提供服务

## 二、整体架构

  **该项目的整体架构分为raft-kvservice对外服务模块、raft-core核心模块、raft-store存储模块三部分。**

<img src=".\readme-pitcher\架构.jpg" style="zoom:150%;" />

### 简单介绍：

* 对外服务模块(raft-kvservice)：负责将client的读写请求通过Server中自定义的Decode解析后转发给核心模块进行处理,再由核心模块将处理结果转发给Server，Server通过自定义的Encoder封装响应最终回复client。

* 核心模块(raft-core)：以集群的方式启动(默认3节点),在短时间通过选举产生一个Leader节点和多个Follower节点,由Leader节点负责接收来自对外服务模块的处理请求,同时将该命令已广播的形式复制到集群内的所有节点。在所有节点回复复制成功后,由Leader发送持久化请求给所有节点将数据持久化。至此,核心模块回复对外服务模块处理后的结果。

* 存储模块(raft-store)：接收到来自核心模块的数据后，将数据写入基于磁盘的WAL(write ahead log)中以防还未持久化就宕机导致数据丢失,先写入WAL可以在恢复后回滚数据。然后写入MemTable(基于内存的红黑树)中,在MemTable中的数据达到阈值后,将其异步交的给SSTable(一个SSTable对应一个文件)。SSTable中包括了自定义的文件头信息、稀疏索引、布隆过滤器、以及真正的数据。

  **接下来将介绍以上三个模块的详细设计**

## 三、详细设计

### （一）对外服务模块(raft-kvservice)

​		1.对外服务模块的设计主要分为Client和Server两部分。由于在Raft中,每个核心节点(Node)都会有可能成为Leader,而Leader既需要管理整个集群内的日志复制工作还需要接收来自客户端的命令,所以在核心节点启动时也需要同时启动对外服务端口(Server)来接收来自客户端的请求(如果非Leader节点收到了读写请求会自动重定向到集群内的Leader节点来处理该请求)。

``` java
//启动Server服务器
public void start() throws Exception {
        //同时启动核心节点服务
        this.node.start();
    	//启动对外服务器监听服务端口
        ServerBootstrap serverBootstrap = new ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new Decoder());//解析请求
                        pipeline.addLast(new Encoder());//封装响应
                        pipeline.addLast(new ServiceHandler(service));
                    }
                });
        logger.info("对外服务端口为： {}", this.port);
        serverBootstrap.bind(this.port);
    }
```



​		2.Client在启动时需要获得集群内活跃Server的Endpoint(NodeId,Host,Port)信息，通过这些Endpoint构建一个连接客户端的通道SocketChannel保存到核心模块中的ServerRouter路由表中。目的是在发送命令时，Client无需关注接收端是谁，路由表会根据Endpoint所对应的核心节点的所处状态来选择合适的进行发送(一般情况下由Leader节点接收)。以下是ServerRouter的结构。

![ServerRouter](.\readme-pitcher\ServerRouter.jpg)

``` java
// 构建服务路由表
private ServerRouter buildServerRouter(Map<NodeId, Address> serverMap) {
        ServerRouter router = new ServerRouter();
        for (NodeId nodeId : serverMap.keySet()) {
            Address address = serverMap.get(nodeId);
            //构建服务器路由表,路由表实际上是根据Host和port创建了一个连接管道
            //A,localhost,3333 B,localhost,3334 C,localhost,3335
            router.add(nodeId, new SocketChannel(address.getHost(), address.getPort()));
        }
        return router;
    }
```

``` java
 //发送消息
    public Object send(Object payload){
        //遍历服务路由表内所有节点(在某个节点返回未响应时，发送到其他节点)
        for (NodeId nodeId : getCandidateNodeIds()) {
            try {
                Object result = doSend(nodeId, payload);
                this.leaderId = nodeId;
                return result;
            }catch (Exception e) {
                // 连接失败，尝试下一个节点
                logger.debug("failed to process with server " + nodeId + ", cause " + e.getMessage());
            }
        }
        throw new NoAvailableServerException("no available server");
    }
```

​		3.由于是使用Tcp进行通信,消息可能会出现半包/粘包的问题,所以在对消息序列化和反序列化时需要特别处理，而不是仅仅转换数据。我这里给出的解决办法是在消息前加上4字节表示消息类型,4字节表示消息长度,其余字节表示消息内容。

![](.\ClientMessage.png)

``` java
private void write(OutputStream output, int messageType, MessageLite message) throws IOException {
        DataOutputStream dataOutput = new DataOutputStream(output);
        byte[] messageBytes = message.toByteArray();
        dataOutput.writeInt(messageType);           //4字节 数据类型
        dataOutput.writeInt(messageBytes.length);   //4字节 数据长度
        dataOutput.write(messageBytes);             //不定长 数据内容
        dataOutput.flush();                         
    }
//反序列化代码太长就不在这里展示~
```

​		4.大概运行流程

![](.\readme-pitcher\raft-kvservice的流程.png)

### （二）核心模块(raft-core)





### （三）存储模块(raft-store)

存储模块是客户端的命令到达的最后一个模块，该模块主要由自己独立设计的LSM-Tree构成。关于Lsm-Tree，推荐一本书[《Designing Data-Intensive Applications》](http://shop.oreilly.com/product/0636920032175.do)(DDIA),这本书由浅入深详细的阐述了Lsm-Tree是一个什么样的数据结构。

> 为什么选择Lsm-Tree而不是B+Tree?
>
> 



## 四、整体流程



## 五、build



## 六、总结



