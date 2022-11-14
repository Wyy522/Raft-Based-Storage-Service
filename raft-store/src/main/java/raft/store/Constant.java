package raft.store;

public class Constant {

    //数据内容
    public static final int PAGE_SIZE = 6300;//控制在两页内
    public static final int TEST_PAGE_MAX=970;
    public static final int TEST_MERGE_PAGE_MAX = 3700;
    public static final int TEST_THRESHOLD_SIZE=1024;
    public static final int TEST_PAGE_SIZE = 4096;
    public static final int META_DATA_SIZE = 20;//4+4+8+4
    public static final String PATH = "src/LsmTreeTestFile";
}
