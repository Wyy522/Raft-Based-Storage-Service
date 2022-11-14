package raft.store;

import com.alibaba.fastjson.JSON;
import raft.store.utils.FileUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static raft.store.Constant.*;
import static raft.store.utils.BufferUtils.byteBufferToString;

public class SSTable {
    private static int levelNumb;
    private static final String SSTable_FILE_NAME = "sst";
    private int numb;
    private String path;
    private RandomAccessFile reader;
    private ParseIndex parseIndex;
    ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public SSTable(String path) throws FileNotFoundException {
        this.path = path;
        reader = new RandomAccessFile(FileUtils.buildFileName(path, String.valueOf(levelNumb), String.valueOf(numb), SSTable_FILE_NAME), "rw");
    }

    //持久化
    public void persistent(TreeMap<String, Command> memTable, String path) throws IOException {
        //获得文件名称
        String filename = FileUtils.buildFileName(path, String.valueOf(levelNumb), String.valueOf(numb), SSTable_FILE_NAME);
        //获得读写文件句柄
        RandomAccessFile writer = new RandomAccessFile(filename, "rw");
        //创建稀疏索引
        parseIndex = new ParseIndex();
        //页号
        int pageNumb = 1;
        //单页数据最大值
        int dataPageSize = 0;
        //所有页中数据总大小
        int dataSize = 0;
        //从1024字节开始写
        writer.seek((long) pageNumb * TEST_THRESHOLD_SIZE);

        //写入数据
        //1.按页(1024)写入,每条数据前四字节是该条数据总大小,后面是具体数据
        //2.记录每一页最后一条数据到稀疏索引中
        for (Command c : memTable.values()) {
            byte[] json = JSON.toJSONBytes(c);
            //一条数据的整体长度
            int len = 4 + json.length;
            dataSize = dataSize + len;
            //单页长度
            dataPageSize = dataPageSize + len;
            if (dataPageSize >= TEST_PAGE_MAX) {
                //记录最后一条Key(key,value,单页长度,页号)
                parseIndex.addIndex(c.getKey(), c.getValue(), dataPageSize, pageNumb);
                dataPageSize = 0;
                //从2*1024开始写,以此类推
                writer.seek((long) ++pageNumb * TEST_THRESHOLD_SIZE);
            }
            //写数据
            writer.writeInt(json.length);
            writer.write(json);
        }

        //记录最后的最后一条Key
        if (dataPageSize >= 0) {
            parseIndex.addIndex(memTable.lastEntry().getKey(), memTable.lastEntry().getValue().getValue(), dataPageSize, pageNumb);
        }

        //从第一页0位置开始写元数据
        writer.seek(0);
        //写入元数据
        SSTableMetaData ssTableMetaData = new SSTableMetaData(numb++, levelNumb, PAGE_SIZE, pageNumb * PAGE_SIZE);
        writer.write(ssTableMetaData.toByteArray());

        //紧接着写入稀疏索引
        //写入稀疏索引
        writer.writeInt(parseIndex.toByteLength());
        writer.write(parseIndex.toByteArray());
        writer.close();
    }

    public void loadToMemory(String path, int levelNumb, int numb, List<SSTableToMem> ssTableToMemS) throws IOException {
        //获得写文件句柄
        reader = new RandomAccessFile(FileUtils.buildFileName(path, String.valueOf(levelNumb), String.valueOf(numb), SSTable_FILE_NAME), "r");
        int Length = 0;
        //读取元数据(20Bytes)
        System.out.println("log(" + levelNumb + "-" + numb + "文件) :正在读取该文件数据");
        System.out.print("log(" + levelNumb + "-" + numb + "文件) :元数据解析结构为==>");
        SSTableMetaData ssTableMetaData = parseSSTableMetaData();
        System.out.println(ssTableMetaData.toString());
        //读取稀疏索引
//        System.out.println("稀疏索引解析结果为---------------------");
        List<ParseIndex.SparseIndexItem> sparseIndexItems = parseIndexToList();
        System.out.println("log(" + levelNumb + "-" + numb + "文件) :该文件稀疏索引解析完成");
        //读取数据(每TEST_THRESHOLD_SIZE大小一个MemTable)
//        System.out.println("数据解析结果为---------------------");
//        List<MemTable> memTables = parseData(sparseIndexItems.size(),ssTableMetaData.getNumb());
        List<MemTable> memTables = parseData(sparseIndexItems.size(), ssTableMetaData.getNumb(), Length);
        SSTableToMem ssTableToMem = new SSTableToMem(ssTableMetaData, sparseIndexItems, memTables);
        System.out.println("log(" + levelNumb + "-" + numb + "文件) :该文件数据解析完成");
        ssTableToMemS.add(ssTableToMem);
        System.out.println("log(" + levelNumb + "-" + numb + "文件) :文件读取完毕");
    }

    public SSTableMetaData parseSSTableMetaData() throws IOException {
        //读取4字节为该文件是这一层第几个
        int metaNumb = reader.readInt();
        //读取4字节为这是第几层文件
        int metaLevel = reader.readInt();
        //读取8字节为数据偏移量
        long metaDataOffset = reader.readLong();
        //读取4字节为该数据总长度
        int metaDataLen = reader.readInt();
        return new SSTableMetaData(metaNumb, metaLevel, metaDataOffset, metaDataLen);
    }

    public List<ParseIndex.SparseIndexItem> parseIndexToList() throws IOException {
        //读取4字节获得稀疏索引的总长度
        int parseIndexLength = reader.readInt();
        //分配对应内存空间
        ByteBuffer parseIndexBuffer = ByteBuffer.allocate(parseIndexLength);
        //读取到指定内存中
        reader.read(parseIndexBuffer.array(), 0, parseIndexLength);
        //讲该byte数组转换为String(重要)
        String s = byteBufferToString(parseIndexBuffer);
        return JSON.parseArray(s, ParseIndex.SparseIndexItem.class);
    }

    public List<MemTable> parseData(int pageNumb, int numb) throws IOException {

        ArrayList<MemTable> memTables = new ArrayList<>();
        //读取所有页里的数据到内存中
        for (int i = 1; i <= pageNumb; i++) {
            MemTable memTable = new MemTable(numb);
            //跳转下一页
            reader.seek((long) i * TEST_THRESHOLD_SIZE);
            try {
                while (true) {
                    //读取4字节获得该条数据长度
                    int dataLength = reader.readInt();
                    //分配对应内存空间
                    ByteBuffer bytes = ByteBuffer.allocate(dataLength);
                    //读取到指定内存中
                    reader.read(bytes.array());
                    //将该byte数组转换为Command对象
                    Command cmd = JSON.parseObject(bytes.array(), 0, dataLength, StandardCharsets.UTF_8, Command.class);
                    //如果读取到该页最后就换到下一页
                    if (cmd == null) {
                        break;
                    }
                    //按顺序(TreeMap)放入内存中
                    memTable.puts(cmd);
                }
            } catch (Exception e) {
                //关闭文件流
                reader.close();
            }
            memTables.add(memTable);
        }
        return memTables;
    }

    public List<MemTable> parseData(int pageNumb, int numb, int length) throws IOException {

        ArrayList<MemTable> memTables = new ArrayList<>();
        //读取所有页里的数据到内存中
        for (int i = 1; i <= pageNumb; i++) {
            MemTable memTable = new MemTable(numb);
            //跳转下一页
            reader.seek((long) i * TEST_THRESHOLD_SIZE);
            try {
                while (true) {
                    //读取4字节获得该条数据长度
                    int dataLength = reader.readInt();
                    //分配对应内存空间
                    ByteBuffer bytes = ByteBuffer.allocate(dataLength);
                    //读取到指定内存中
                    reader.read(bytes.array());
                    //将该byte数组转换为Command对象
                    Command cmd = JSON.parseObject(bytes.array(), 0, dataLength, StandardCharsets.UTF_8, Command.class);
                    //如果读取到该页最后就换到下一页
                    if (cmd == null) {
                        break;
                    }
                    //按顺序(TreeMap)放入内存中
                    memTable.puts(cmd);

                    //记录长度
                    length += cmd.getLength();
                }
            } catch (Exception e) {
                //关闭文件流
                reader.close();
            }
            memTables.add(memTable);
        }

//        System.out.println("数据长度为 :"+length);
        return memTables;
    }

    public ParseIndex loadIndexToMemory(String path, int levelNumb, int numb) throws IOException {
        reader = new RandomAccessFile(FileUtils.buildFileName(path, String.valueOf(levelNumb), String.valueOf(numb), SSTable_FILE_NAME), "r");
        reader.seek(20);
        List<ParseIndex.SparseIndexItem> sparseIndexItems = parseIndexToList();
        ParseIndex parseIndex = new ParseIndex();
        parseIndex.setIndexItems(sparseIndexItems);
        return parseIndex;
    }

    public MemTable loadOnePageToMemory(String path, int levelNumb, int numb, int pageNumb) throws IOException {
        reader.seek((long) pageNumb * TEST_THRESHOLD_SIZE);
        MemTable memTable = new MemTable(numb);
        while (true) {
            try {  //读取4字节获得该条数据长度
                int dataLength = reader.readInt();
                //分配对应内存空间
                ByteBuffer bytes = ByteBuffer.allocate(dataLength);
                //读取到指定内存中
                reader.read(bytes.array());
                //将该byte数组转换为Command对象
                Command cmd = JSON.parseObject(bytes.array(), 0, dataLength, StandardCharsets.UTF_8, Command.class);
                //如果读取到该页最后就换到下一页
                if (cmd == null) {
                    break;
                }
                //按顺序(TreeMap)放入内存中
                memTable.puts(cmd);
            } catch (Exception e) {
                //关闭文件流
                reader.close();
                break;
            }
        }
        return memTable;
    }

    public void levelAdd() {
        levelNumb = levelNumb + 1;
        numb = 0;
    }

    public static int getLevelNumb() {
        return levelNumb;
    }

    public static void setLevelNumb(int levelNumb) {
        SSTable.levelNumb = levelNumb;
    }

    public int getNumb() {
        return numb;
    }

    public void setNumb(int numb) {
        this.numb = numb;
    }
}
