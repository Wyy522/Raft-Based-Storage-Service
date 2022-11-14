package raft.store;

import com.alibaba.fastjson.JSON;

public class Command implements Comparable<Command> {
    public static final int OP_SET = 1;
    public static final int OP_GEt = 2;
    public static final int OP_RM = 3;

    private int op;
    private String key;
    private String value;
    private int numb;


    public Command(int op, String key, String value) {
        this(op, key, value, -1);
    }

    public Command(int op, String key, String value, int numb) {
        this.op = op;
        this.key = key;
        this.value = value;
        this.numb = numb;
    }

    public int getLength(){
        return 4+key.length()+value.length()+4;
    }

    public int getNumb() {
        return numb;
    }

    public void setNumb(int numb) {
        this.numb = numb;
    }

    public int getBytes(Command command) {
        return JSON.toJSONBytes(command).length;
    }

    public int getOp() {
        return op;
    }

    public void setOp(int op) {
        this.op = op;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "Command{" +
                "op=" + op +
                ", key='" + key + '\'' +
                ", value='" + value + '\'' +
                ", numb=" + numb +
                '}';
    }

    //如果按字典顺序 this.key 位于 o.key 参数之前，比较结果为一个负整数；如果 this.key 位于 o.key 之后，比较结果为一个正整数；如果两个字符串相等，则结果为 0。
    @Override
    public int compareTo(Command o) {
        if (this.getKey().equals(o.getKey()) && this.getNumb() < o.getNumb()) {
            return -1;
        }
        return 1;
    }
}
