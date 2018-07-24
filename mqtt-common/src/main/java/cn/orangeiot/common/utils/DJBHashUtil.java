package cn.orangeiot.common.utils;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-03-19
 */
public class DJBHashUtil {


    /**
     * @Description hash分区
     * @author zhang bo
     * @date 18-3-19
     * @version 1.0
     */
    public static int Time33(String str) {
        int len = str.length();
        int hash = 0;
        for (int i = 0; i < len; i++)
            // (hash << 5) + hash 相当于 hash * 33
            hash = (hash << 5) + hash + (int) str.charAt(i);
        return Math.abs(hash);
    }
}
