package raft.store;

import com.google.common.eventbus.EventBus;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static raft.store.Constant.TEST_MERGE_PAGE_MAX;


public class MemTable {

    TreeMap<String, Command> memTable;
    private int levelNumb;
    private int numb;
    private int memTableLength;
    private volatile boolean isImmTable = false;


    EventBus eventBus;
    ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public MemTable() {
        this(new EventBus());
    }

    public MemTable(int numb) {
        this(new EventBus(), numb);
    }

    public MemTable(EventBus eventBus) {
        this(eventBus, 0);
    }

    public MemTable(EventBus eventBus, int numb) {
        this.memTable = new TreeMap<String, Command>();
        this.eventBus = eventBus;
        this.numb = numb;
    }


    public boolean puts(Command command) {
        try {
            lock.writeLock().lock();
            memTableLength += command.getBytes(command);
            if (memTableLength <= TEST_MERGE_PAGE_MAX) {
                memTable.put(command.getKey(), command);
                return true;
            } else {
                memTableLength = 0;
                eventBus.post(memTable);
                return false;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }


    public void mergePut(MemTable memTable, Command command) {
        memTable.memTable.put(command.getKey(), command);
    }

    public void mergePersistent(MemTable memTable) {
        eventBus.post(memTable.getMemTable());
    }


    //如果按字典顺序 this.key 位于 o.key 参数之前，比较结果为一个负整数；如果 this.key 位于 o.key 之后，比较结果为一个正整数；如果两个字符串相等，则结果为 0。
    public MemTable compare(MemTable m0) {
        try {
            MemTable m1 = this;
            //可优化(没有落盘page数据)
            for (Map.Entry<String, Command> e : m1.memTable.entrySet()) {
                e.getValue().setNumb(m1.getNumb());
            }
            //遍历较小的MemTable
            for (Map.Entry<String, Command> e : m0.memTable.entrySet()) {
                //如果较小MemTable存在和较大MemTable一样的Key,则删除该Entry
                if (this.memTable.containsKey(e.getKey())) {
                    m0.memTable.remove(e.getKey());
                    continue;
                }
                //把页号存入Command
                e.getValue().setNumb(m0.getNumb());
                //如果不存在则存入较大的MemTable(有序)
                m1.puts(e.getValue());
            }
            return m1;
        } finally {
        }
    }

    public void clear() {
        memTable.clear();
    }

    public int getMemTableLength() {
        return memTableLength;
    }

    public TreeMap<String, Command> getMemTable() {
        return memTable;
    }

    public void setMemTable(TreeMap<String, Command> memTable) {
        this.memTable = memTable;
    }

    public int getLevelNumb() {
        return levelNumb;
    }

    public void setLevelNumb(int levelNumb) {
        this.levelNumb = levelNumb;
    }

    public int getNumb() {
        return numb;
    }

    public void setNumb(int numb) {
        this.numb = numb;
    }

    public void setMemTableLength(int memTableLength) {
        this.memTableLength = memTableLength;
    }

    public boolean getIsImmTable() {
        return isImmTable;
    }

    public void setImmTable(boolean immTable) {
        isImmTable = immTable;
    }

    @Override
    public String toString() {
        return "MemTable{" +
                "memTable=" + memTable +
                ", levelNumb=" + levelNumb +
                ", numb=" + numb +
                ", memTableLength=" + memTableLength +
                '}';
    }


}
