package raft.store;

import com.alibaba.fastjson.JSON;

import java.util.ArrayList;
import java.util.List;

import static raft.store.Constant.PAGE_SIZE;


public class ParseIndex {
    private List<SparseIndexItem> indexItems;
    private int length;

    public ParseIndex() {
        indexItems = new ArrayList<>();
    }

    public void addIndex(String key, String value, int len, int pageNumb) {
        SparseIndexItem item = new SparseIndexItem(key, value, len, pageNumb);
        indexItems.add(item);
        length++;
    }

    public byte[] toByteArray() {
        return JSON.toJSONBytes(indexItems);
    }


    public List<SparseIndexItem> getIndexItems() {
        return indexItems;
    }


    public int toByteLength() {
        return JSON.toJSONBytes(indexItems).length;
    }

    public void Println() {
        for (SparseIndexItem sparseIndexItem : indexItems) {
            String s = sparseIndexItem.toString();
            System.out.println(s);
        }
    }

    public void setIndexItems(List<SparseIndexItem> indexItems) {
        this.indexItems = indexItems;
    }

    @Override
    public String toString() {
        return "ParseIndex{" +
                "indexItems=" + indexItems +
                ", length=" + length +
                '}';
    }

    public static class SparseIndexItem {
        String key;
        String value;
        int pageNumb;
        long offset;
        int len;
        //TODO bolon filter

        public SparseIndexItem(String key, String value, int len, int pageNumb) {
            this.key = key;
            this.value = value;
            this.pageNumb = pageNumb;
            this.offset = (long) pageNumb * PAGE_SIZE;
            this.len = len;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public int getPageNumb() {
            return pageNumb;
        }

        public void setPageNumb(int pageNumb) {
            this.pageNumb = pageNumb;
        }

        public long getOffset() {
            return offset;
        }

        public void setOffset(long offset) {
            this.offset = offset;
        }

        public int getLen() {
            return len;
        }

        public void setLen(int len) {
            this.len = len;
        }


        @Override
        public String toString() {
            return "SparseIndexItem{" +
                    "key='" + key + '\'' +
                    ", offset=" + offset +
                    ", len=" + len +
                    '}';
        }
    }
}
