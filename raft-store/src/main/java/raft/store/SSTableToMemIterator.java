package raft.store;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class SSTableToMemIterator implements Iterator<SSTableToMem> {
    List<SSTableToMem> ssTableToMemS;

    public SSTableToMemIterator() {
        this.ssTableToMemS = new ArrayList<>();
    }

    public int getLength() {
        int length=0;
        for (SSTableToMem s : ssTableToMemS) {
            for (MemTable memTable : s.getMemTables()) {
                for (Map.Entry<String, Command> c : memTable.memTable.entrySet()) {
                    length += c.getValue().getLength();
                }
            }
        }
        return length;
    }

    @Override
    public boolean hasNext() {
        return ssTableToMemS.iterator().hasNext();
    }

    @Override
    public SSTableToMem next() {
        return ssTableToMemS.iterator().next();
    }

    @Override
    public String toString() {
        return "SSTableToMemIterator{" +
                "ssTableToMemS=" + ssTableToMemS +
                '}';
    }
}
