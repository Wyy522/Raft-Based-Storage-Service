# 基于Raft协议的分布式键值存储服务

## 一、简介

​         该项目参考《In Search of an Understandable Consensus Algorithm》论文实现Raft协议中三个子问题中的前两个，领导人选举(Leader election)和日志复制(Log replication)。使用网络编程框架Netty处理核心节点之间的RPC(RequestVote RPC和AppendEntries RPC)请求交互，核心节点同时也开启对外服务端口等待客户端连接发送命令。接收到来自客户端的Get、Set命令后Leader节点通过AppendEntries RPC保证集群内节点将命令中的数据以Lsm-tree的存储策略持久化落盘。

> 分布式共识算法那么多,为什么使用Raft ?
>
> 答：在学术理论界,最闪耀的当属的**Paxos**,但将其工程落地相当复杂,绝大多数人认为其难以理解,晦涩难懂(这一点在上述提到的论文中也有阐述),本人同样也是对其一知半解且资料也相对较少，故放弃Paxos。相比 **ZAB**,Raft 的设计更为简洁。以可理解性和易于实现为目标的Raft算法极大的帮助了我们对分布式共识算法的理解,也正是易于实现让我选择了Raft。
>
> 什么是分布式系统，分布式与集群的区别？
>
> 答：由于普通的单机服务器面对业务量巨大的场景下无法满足要求,垂直扩展升级机器硬件和水平扩展堆廉价服务器是两种最常见的解决方案,目前互联网领域绝大多数选择了后者水平扩展,也就是加机器。分布式(distributed): 在多台不同的服务器中部署不同的服务模块,通过远程调用协同工作,对外提供服务。集群(cluster): 在多台不同的服务器中部署相同的应用和服务模块,通过负载均衡设备对外提供服务

## 二、整体架构

  **该项目的整体架构分为raft-kvservice对外服务模块、raft-core核心模块、raft-store存储模块三部分。**

<img src=".\readme-pitcher\架构.jpg" alt="架构" style="zoom:150%;" />

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



<img src=".\readme-pitcher\ServerRouter.jpg" alt="ServerRouter" style="zoom:100%;" />

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

<img src=".\readme-pitcher\ClientMessage.png" alt="ClientMessage" style="zoom:100%;" />

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

<img src=".\readme-pitcher\raft-kvservice的流程.png" alt="raft-kvservice的流程" style="zoom:100%;" />

### （二）核心模块(raft-core)





### （三）存储模块(raft-store)	

​		存储模块是客户端的命令到达的最后一个模块，该模块主要由自己独立设计的LSM-Tree构成。关于Lsm-Tree，推荐一本书[《Designing Data-Intensive Applications》](http://shop.oreilly.com/product/0636920032175.do)(DDIA),这本书的第三章节由浅入深详细的阐述了Lsm-Tree，值得一看！

> 为什么选择Lsm-Tree而不是B+Tree?

<img src=".\readme-pitcher\Lsm-tree架构.jpg" alt="Lsm-Tree架构.png" style="zoom:100%;" />

#### 	1.WAL(write ahead log)

​			数据到达存储模块的第一站便是WAL预写日志中,由于预写日志是直接写入磁盘的,由于数据持久化的过程可能比较费时,所以有可能会在持久化的过程中突然发送宕机或者断电,从而导致数据丢失，而预先写入WAL中便可以在系统修复后重新加载到内存持久化的。该项目中的WAL设计较为简单(拿到数据就往文件里怼)。

```java
public void set(String key, String value) throws IOException, InterruptedException {
        Command command = new Command(1, key, value);
        wal.write(command);//接到数据就写WAL
        if (!memTable.puts(command)) {
            this.memTable = new MemTable(eventBus);
            memTable.puts(command);
        }
    }
```

```java
public void write(Command command) throws IOException {
        byte[] commandBytes = JSON.toJSONBytes(command);
        writer.writeInt(commandBytes.length);	//4字节  写入数据长度
        writer.write(commandBytes);				//不定长 写入二进制数据
    }
```

<img src=".\readme-pitcher\wal-store.png" alt="wal-store.png" style="zoom:100%;" />

#### 	2.MemTable

​			数据写完WAL后会立即写入到MemTable中,是基于内存实现的有序集合,可以有多种实现方式。该项目直接使用java.util包下原生的TreeMap来实现MemTable这一组件。TreeMap的底层是红黑树，可以按照自然顺序进行排序或者根据创建映射时提供的Comparator接口进行排序。从存储角度而言，这比HashMap的O(1)时间复杂度要差些；但是在统计性能上，TreeMap同样可以保证log(N)的时间开销，这又比HashMap的O(N)时间复杂度好不少。

``` java
public class MemTable {
    TreeMap<String, Command> memTable;
    private int levelNumb;//层号
    private int numb;//页号
    private int memTableLength;//当前MemTable大小(用来判断是否达到阈值)
    private volatile boolean isImmTable = false;//判断当前MemTable是否正在持久化
}
```



>  为什么必须要是有序集合？
>
> 答：是为了保证持久化到SSTable中时仍能保持顺序性。我们通常会给SSTable中加上稀疏索引，这样便可以快速通过索引来定位到数据所在的范围，再通过二分查找快速锁定数据。

当MemTable中的数据达到一定的阈值时会异步的进行Flush操作,同时再创建一个新的MemTable用来接收数据。通过上面Lsm-Tree的架构图可以得知第一次Flush操作称为Minor Compaction,而之后对SSTable的操作称为Major Compactionm。这是因为首次Flush并不会对MemTable中的数据进行去重操作(体现Lsm-tree的快)，也就是说存入磁盘里的数据有可能是重复的。而之后对SSTable操作是需要去重的，一是为了节省空间，二是为了加速查询。

####     3.SSTable

​		数据的最后一站就是SSTable,也是存储模块中最复杂的一部分(当然离工程化还差很远)。该项目所设计的SSTable分为四部分,分别为元数据、稀疏索引、布隆过滤器以及数据区,一个SSTable对应一个实实在在4K大小的文件。元数据和稀疏索引通常是被放置在前1K个字节中的,而后面的3K个字节存放真正的数据。

<img src=".\readme-pitcher\SSTable.jpg" alt="SSTable.png" style="zoom:100%;" />

> 为什么要这样设计？
>
> 答：由于磁盘的存取单位是一个一个4K大小的块,所以当以4的整倍数存取数据时对于磁盘来说是最高效的。同时也为了下面优化时可以并行的进行Merge。

​		(1)首先是元数据的设计,其包括了当前SSTable是第几层的第几个的(这在Merge时有重要作用),也包括了数据的基本信息。固定其大小为20字节方便找到稀疏索引快速查询。

​	<img src=".\readme-pitcher\MetaData.png" alt="MetaData.png" style="zoom:100%;" />

```java
public class SSTableMetaData {
    private int numb;//当前层的第几个文件(numb越大数据越新)
    private int level;//第几层的文件(level越大数据越旧)
    private long dataOffset;//数据偏移量
    private int dataLen;//数据总长度
}
```



​		(2)其次是稀疏索引的设计，稀疏索引记录了每1K数据的第一条数据的内容、页号、偏移量、长度。以便在查询直接判断Key值快速锁定数据范围,通过偏移量直接读取数据。

<img src=".\readme-pitcher\ParseIndex.jpg" alt="ParseIndex.png" style="zoom:100%;" />

```java
public  class SparseIndexItem {
     private  String key;//键
     private  String value;//值
     private  int pageNumb;//页号
     private  long offset;//数据偏移量
     private  int len;//长度
}
```

​		(3)最后是数据的设计,一条数据包括了操作类型,Key值，Value值，以及系统生成的页号。

<img src=".\readme-pitcher\Data.png" alt="Data.png" style="zoom:100%;" />

``` java
public class Command{
    private int op;//操作类型 支持GET SET DEL
    private String key;//键
    private String value;//值
    private int numb;//页号

}
```



#### 4.写入流程

#### 5.查询流程

#### 6.合并流程





## 四、整体流程

## 五、build

## 六、总结
