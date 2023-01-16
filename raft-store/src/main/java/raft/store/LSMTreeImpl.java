package raft.store;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import java.io.IOException;
import java.util.*;



public class LSMTreeImpl {
    private String path;
    private Boolean isRunning;
    EventBus eventBus = new EventBus();
    private SSTable ssTable;
    private MemTable memTable;
    private WAL wal;
    private List<MemTable> afterMergeMemTable;
    SSTableToMemIterator ssTableToMemIterator;

    public LSMTreeImpl(String path) throws IOException {
        this.path = path;
        this.memTable = new MemTable(eventBus);
        this.ssTable = new SSTable(path);
        this.wal = new WALImpl(path);
        this.isRunning = false;
        this.eventBus.register(this);
        this.ssTableToMemIterator = new SSTableToMemIterator();
        this.afterMergeMemTable = new ArrayList<>();
    }

    public void start() throws IOException {
        System.out.println("log :LsmTree Start...");
        this.isRunning = true;
        reload();
        Thread thread = new Thread(() -> {
            while (isRunning) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }


    public void stop() throws IOException {
        this.isRunning = false;
        System.out.println("log :LsmTree Stop...");
    }

    public void reload() throws IOException {
        wal.readSeek(0);
        while (true) {
            try {
                Command command = wal.read();
                this.memTable.puts(command);

            }catch (Exception e){
              break;
            }
        }
        //TODO load SSTableMetaData to memory
    }

    @Subscribe
    private void doMemTablePersist(TreeMap<String, Command> memTable) throws IOException {
        System.out.println("log :正在持久化");
        Thread thread = new Thread(() -> {
            try {
                ssTable.persistent(memTable, path);
                wal.clear();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        thread.start();
    }

    public void set(String key, String value) throws IOException, InterruptedException {
        Command command = new Command(1, key, value);
        wal.write(command);
        if (!memTable.puts(command)) {
            Thread.sleep(1000);
            this.memTable = new MemTable(eventBus);
            memTable.puts(command);
        }
    }

    public void merge() throws InterruptedException {
        List<Command> commands = new ArrayList<>();
        int AllSSTableToMemIteratorLength = ssTableToMemIterator.getLength();
        System.out.println("log :merge前所有加载到内存的数据长度为 " + AllSSTableToMemIteratorLength);
        for (int j = 0; j < ssTableToMemIterator.ssTableToMemS.size() - 1; j=j+2) {
            ssTableToMemIterator.ssTableToMemS.get(j).compare(ssTableToMemIterator.ssTableToMemS.get(j + 1), afterMergeMemTable);
        }

        for (MemTable m : afterMergeMemTable) {
            for (Map.Entry<String, Command> entry : m.getMemTable().entrySet()) {
                commands.add(entry.getValue());
            }
        }
        //归并排序去重操作规则为:若两个Key相等取Numb较大的;
        Arrays.sort(commands.toArray());

        int AllAfterMergeLength = 0;
        for (Command c : commands) {
            AllAfterMergeLength += c.getLength();
        }
        ssTable.levelAdd();
        System.out.println("log :正在进行merge操作");
        MemTable memTable = new MemTable(eventBus);
        for (Command c : commands) {
            memTable.mergePut(memTable, c);
        }
        System.out.println("log :merge操作已完成");
        System.out.println("log :merge后的数据长度为 " + AllAfterMergeLength);
        memTable.mergePersistent(memTable);
        ssTableToMemIterator.ssTableToMemS.clear();
//        System.out.println("去重后的结果为---------------------" + commands);
    }

    public Command get(String key) throws IOException {
        System.out.println("log :正在内存中查找");
        Command command = memTable.memTable.get(key);
        //先去内存中找
        if (command != null) {
            System.out.println("log :该值在内存中");
            return command;
        }


        //去levelNumb小的SSTable中找(走稀疏索引)
        System.out.println("log :正在磁盘中查找");
        //如果按字典顺序 p.getKey() 位于 key 参数之前，比较结果为一个负整数；如果  p.getKey() 位于 key 之后，比较结果为一个正整数；如果两个字符串相等，则结果为 0。
        out:
        for (int i = 0; i < 2; i++) {
            //读取稀疏索引
            ParseIndex parseIndex = loadIndexToMemory(0, i);
            for (ParseIndex.SparseIndexItem p : parseIndex.getIndexItems()) {
                //和稀疏索引中的key比较
                int j = p.getKey().compareTo(key);
                if (j >= 0) {
                    MemTable memTable = loadOnePageToMemory(0, i, p.getPageNumb());
                    command = memTable.memTable.get(key);
                    if (command != null) {
                        break out;
                    }
                }
            }
        }

        if (command==null){
            System.out.println("log :未找到该值");
            return null;
        }
        return command;
    }


    public void loadSSTableToMemory(String path, int levelNumb, int numb) throws IOException {
        //存放所有meTable的数组(merge时用)
        ssTable.loadToMemory(path, levelNumb, numb, ssTableToMemIterator.ssTableToMemS);
    }

    public ParseIndex loadIndexToMemory(int levelNumb, int numb) throws IOException {
        return ssTable.loadIndexToMemory(path, levelNumb, numb);
    }

    public MemTable loadOnePageToMemory(int levelNumb, int numb, int pageNumb) throws IOException {
        return ssTable.loadOnePageToMemory(path, levelNumb, numb, pageNumb);
    }

}
