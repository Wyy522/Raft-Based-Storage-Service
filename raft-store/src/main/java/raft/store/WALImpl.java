package raft.store;

import com.alibaba.fastjson.JSON;
import raft.store.utils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
public class WALImpl implements WAL {
    private String path;
    private final String WAL_File_NAME = "wal";

    private final int WAL_OP = -1;
    private RandomAccessFile writer;
    private RandomAccessFile reader;

    public WALImpl(String path) throws IOException {
        this.path = path;
        this.writer = new RandomAccessFile(FileUtils.buildFileName(path, String.valueOf(WAL_OP), String.valueOf(WAL_OP), WAL_File_NAME), "rw");
        this.reader = new RandomAccessFile(FileUtils.buildFileName(path, String.valueOf(WAL_OP), String.valueOf(WAL_OP), WAL_File_NAME), "r");
        writer.seek(writer.length());
    }

    @Override
    public void readSeek(long pos) throws IOException {
        this.reader.seek(pos);
    }

    @Override
    public void write(Command command) throws IOException {
        byte[] commandBytes = JSON.toJSONBytes(command);
        writer.writeInt(commandBytes.length);
        writer.write(commandBytes);
    }

    @Override
    public Command read() throws IOException {
        int size = reader.readInt();
        byte[] buffer = new byte[size];
        this.reader.read(buffer);
        Command command = JSON.parseObject(buffer, Command.class);
        return command;
    }

    @Override
    public void clear() throws IOException {
        this.reader.close();
        this.writer.close();
        File cur = new File(FileUtils.buildFileName(path, String.valueOf(WAL_OP), String.valueOf(WAL_OP), WAL_File_NAME));
        cur.delete();
        cur.createNewFile();
        this.writer = new RandomAccessFile(FileUtils.buildFileName(path, String.valueOf(WAL_OP), String.valueOf(WAL_OP), WAL_File_NAME), "rw");
        this.reader = new RandomAccessFile(FileUtils.buildFileName(path, String.valueOf(WAL_OP), String.valueOf(WAL_OP), WAL_File_NAME), "r");

    }
}
