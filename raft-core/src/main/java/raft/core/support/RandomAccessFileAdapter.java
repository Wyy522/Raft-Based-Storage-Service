package raft.core.support;


import java.io.*;

/**
 * 文件句柄的实现
 * @author yiyewei
 * @create 2022/9/21 0:24
 **/

public class RandomAccessFileAdapter implements SeekableFile {

    private final File file;
    private final RandomAccessFile randomAccessFile;

    //构造器,读写权限
    public RandomAccessFileAdapter(File file) throws FileNotFoundException {
        this(file, "rw");
    }

    //构造器,指定权限
    public RandomAccessFileAdapter(File file, String mode) throws FileNotFoundException {
        this.file = file;
        randomAccessFile = new RandomAccessFile(file, mode);
    }

    //定位
    @Override
    public void seek(long position) throws IOException {
        randomAccessFile.seek(position);
    }

    //写入整数
    @Override
    public void writeInt(int i) throws IOException {
        randomAccessFile.writeInt(i);
    }

    //写入长整数
    @Override
    public void writeLong(long l) throws IOException {
        randomAccessFile.writeLong(l);
    }

    //写入字节数据
    @Override
    public void write(byte[] b) throws IOException {
        randomAccessFile.write(b);
    }

    //读取整数
    @Override
    public int readInt() throws IOException {
        return randomAccessFile.readInt();
    }

    //读取长整数
    @Override
    public long readLong() throws IOException {
        return randomAccessFile.readLong();
    }

    //读取字节数据,返回读取的字节数
    @Override
    public int read(byte[] b) throws IOException {
        return randomAccessFile.read(b);
    }

    //获取文件大小
    @Override
    public long size() throws IOException {
        return randomAccessFile.length();
    }

    //裁剪文件大小
    @Override
    public void truncate(long size) throws IOException {
        randomAccessFile.setLength(size);
    }

    //获取从某个位置开始的输入流
    @Override
    public InputStream inputStream(long start) throws IOException {
        FileInputStream input = new FileInputStream(file);
        if (start > 0) {
            input.skip(start);
        }
        return input;
    }

    //获取当前位置
    @Override
    public long position() throws IOException {
        return randomAccessFile.getFilePointer();
    }

    //刷新
    @Override
    public void flush() throws IOException {
    }

    //关闭文件
    @Override
    public void close() throws IOException {
        randomAccessFile.close();
    }

}
