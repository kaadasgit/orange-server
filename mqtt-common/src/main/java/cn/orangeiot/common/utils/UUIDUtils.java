package cn.orangeiot.common.utils;

import io.vertx.core.json.JsonObject;

import java.util.UUID;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2017-12-11
 */
public class UUIDUtils {

    /**
     * 获得一个UUID
     *
     * @return String UUID
     */
    public static String getUUID() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    /**
     * 获得指定数目的UUID
     *
     * @param number int 需要获得的UUID数量
     * @return String[] UUID数组
     */
    public static String[] getUUID(int number) {
        if (number < 1) {
            return null;
        }
        String[] ss = new String[number];
        for (int i = 0; i < number; i++) {
            ss[i] = getUUID();
        }
        return ss;
    }



    public static void main(String[] args) {
        System.out.println("::" + UUIDUtils.getUUID());
        String[] ss = getUUID(2);
        for (int i = 0; i < ss.length; i++) {
            System.out.println(ss[i]);
        }
    }
}