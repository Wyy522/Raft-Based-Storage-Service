package raft.store.utils;

//文件命名格式:
//wal: wal.log

public class FileUtils {
    public static String buildFileName(String path,String levelNumb,String numb,String filename){
        StringBuilder sb = new StringBuilder(path);

        //判断有没有下一级目录
        //例如 path = "/user/src" ----> path = "/user/src/"
        if (path.charAt(path.length() - 1) != '/') {
            sb.append('/');
        }
        if (!levelNumb.equals("-1")){
            sb.append(filename).append("_").append(levelNumb).append("_").append(numb).append(".log");
            return sb.toString();
        }
        sb.append(filename + ".log");
        return sb.toString();
    }
}
