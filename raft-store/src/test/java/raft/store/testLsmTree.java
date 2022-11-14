package raft.store;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Test;

import java.io.IOException;

import static raft.store.Constant.PATH;

public class testLsmTree {

    @Test
    public void TestLsmTreeDBStart() throws IOException {
        LSMTreeImpl lsmTree=new LSMTreeImpl(PATH);
        lsmTree.start();
        lsmTree.stop();
    }

    @Test
    public void TestLsmTreeDBSetData() throws IOException, InterruptedException {
        LSMTreeImpl lsmTree=new LSMTreeImpl(PATH);
        lsmTree.start();
        for (int i = 0; i <160; i++) {
            lsmTree.set(RandomStringUtils.randomAlphabetic(5),String.valueOf(i));
        }
        lsmTree.stop();
    }


    @Test
    public void TestLsmTreeDBLoadSSTableToMemory() throws IOException {
        LSMTreeImpl lsmTree=new LSMTreeImpl(PATH);
        lsmTree.start();
        lsmTree.loadSSTableToMemory(PATH,0,0);
        lsmTree.loadSSTableToMemory(PATH,0,1);
        lsmTree.stop();
    }

    @Test
    public void TestLsmTreeDBGetData() throws IOException {
        LSMTreeImpl lsmTree=new LSMTreeImpl(PATH);
        lsmTree.start();
        System.out.println("log :该值为"+lsmTree.get("Shnou"));
        lsmTree.stop();
    }

    @Test
    public void TestLsmTreeDBMerge() throws IOException, InterruptedException {
        LSMTreeImpl lsmTree=new LSMTreeImpl(PATH);
        lsmTree.start();
        //先load到内存中
        lsmTree.loadSSTableToMemory(PATH,0,0);
        lsmTree.loadSSTableToMemory(PATH,0,1);
        lsmTree.merge();
        lsmTree.stop();
    }
}
