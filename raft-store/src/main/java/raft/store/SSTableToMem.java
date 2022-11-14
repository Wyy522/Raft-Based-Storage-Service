package raft.store;

import java.util.List;

public class SSTableToMem {
    private SSTableMetaData ssTableMetaData;
    private List<ParseIndex.SparseIndexItem> sparseIndexItem;
    private List<MemTable> memTables;

    public SSTableToMem(SSTableMetaData ssTableMetaData, List<ParseIndex.SparseIndexItem> sparseIndexItem, List<MemTable> memTables) {
        this.ssTableMetaData = ssTableMetaData;
        this.sparseIndexItem = sparseIndexItem;
        this.memTables = memTables;
    }

    public SSTableMetaData getSsTableMetaData() {
        return ssTableMetaData;
    }

    public void setSsTableMetaData(SSTableMetaData ssTableMetaData) {
        this.ssTableMetaData = ssTableMetaData;
    }

    public List<ParseIndex.SparseIndexItem> getSparseIndexItem() {
        return sparseIndexItem;
    }

    public void setSparseIndexItem(List<ParseIndex.SparseIndexItem> sparseIndexItem) {
        this.sparseIndexItem = sparseIndexItem;
    }

    public List<MemTable> getMemTables() {
        return memTables;
    }

    public void setMemTables(List<MemTable> memTables) {
        this.memTables = memTables;
    }

    @Override
    public String toString() {
        return "SSTableToMem{" +
                "ssTableMetaData=" + ssTableMetaData +
                ", sparseIndexItem=" + sparseIndexItem +
                ", memTables=" + memTables +
                '}';
    }

    public void compare(SSTableToMem s0,List<MemTable> afterMergeMemTable) {
        SSTableToMem s1= this;
        int size = s1.getMemTables().size();
        for (int i = 0; i < size; i++) {
            MemTable onePageMergeMemTable = s1.getMemTables().get(i).compare(s0.memTables.get(i));
            afterMergeMemTable.add(onePageMergeMemTable);
        }
    }
}
