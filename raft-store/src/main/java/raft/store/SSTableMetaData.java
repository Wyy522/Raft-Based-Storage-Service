package raft.store;

import java.nio.ByteBuffer;

import static raft.store.Constant.META_DATA_SIZE;

public class SSTableMetaData {
    private int numb;
    private int level;
    private long dataOffset;
    private int dataLen;

    public SSTableMetaData(int numb, int level, long dataOffset, int dataLen) {
        this.numb = numb;
        this.level = level;
        this.dataOffset = dataOffset;
        this.dataLen = dataLen;
    }

    public byte[] toByteArray() {
        ByteBuffer buffer = ByteBuffer.allocate(META_DATA_SIZE);
        buffer.putInt(numb);
        buffer.putInt(level);
        buffer.putLong(dataOffset);
        buffer.putInt(dataLen);
        return buffer.array();
    }

    @Override
    public String toString() {
        return "SSTableMetaData{" +
                "numb=" + numb +
                ", level=" + level +
                ", dataOffset=" + dataOffset +
                ", dataLen=" + dataLen +
                '}';
    }

    public int getNumb() {
        return numb;
    }

    public void setNumb(int numb) {
        this.numb = numb;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public long getDataOffset() {
        return dataOffset;
    }

    public void setDataOffset(long dataOffset) {
        this.dataOffset = dataOffset;
    }

    public int getDataLen() {
        return dataLen;
    }

    public void setDataLen(int dataLen) {
        this.dataLen = dataLen;
    }
}
