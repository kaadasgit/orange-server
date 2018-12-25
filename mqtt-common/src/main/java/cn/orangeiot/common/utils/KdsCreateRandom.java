package cn.orangeiot.common.utils;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Random;

/**
 * @author :   luayuxiang
 * @fileName :   com.Kaadas.KdsCreateRandom.java
 * <p>
 * <p>
 * <p>
 * date   |   author   |   version   |   revise   |
 * 2017年3月28日 |   windows   |     1.0     |            |
 * @describe :
 * <p>
 * ALL RIGHTS RESERVED,COPYRIGHT(C) Kaadas 2017
 */

public class KdsCreateRandom {

    private static Logger logger = LogManager.getLogger(KdsCreateRandom.class);


    /**
     * 创建指定数量的随机字符串
     *
     * @param length
     * @return
     */
    public static String createRandom(int length) {
        String retStr = "";
        String strTable = "1234567890";
        int len = strTable.length();
        boolean bDone = true;
        do {
            retStr = "";
            int count = 0;
            for (int i = 0; i < length; i++) {
                double dblR = Math.random() * len;
                int intR = (int) Math.floor(dblR);
                char c = strTable.charAt(intR);
                if (('0' <= c) && (c <= '9')) {
                    count++;
                }
                retStr += strTable.charAt(intR);
            }
            if (count >= 2) {
                bDone = false;
            }
        } while (bDone);
        return retStr;
    }

    /**
     * @Description 生成随机数
     * @author zhang bo
     * @date 18-1-23
     * @version 1.0
     */
    public static String getItemID(int n) {
        String val = "";
        Random random = new Random();
        for (int i = 0; i < n; i++) {
            String str = random.nextInt(2) % 2 == 0 ? "num" : "char";
            if ("char".equalsIgnoreCase(str)) { // 产生字母
                int nextInt = random.nextInt(2) % 2 == 0 ? 65 : 97;
                val += (char) (nextInt + random.nextInt(26));
            } else if ("num".equalsIgnoreCase(str)) { // 产生数字
                val += String.valueOf(random.nextInt(10));
            }
        }
        return byte2Hex(val.getBytes());
    }

    public static void main(String[] args) {
        String time = randomHexString(24);
        System.out.println(time);

    }

    /**
     * 将byte转为16进制
     *
     * @param bytes
     * @return
     */
    @SuppressWarnings("Duplicates")
    private static String byte2Hex(byte[] bytes) {
        StringBuffer stringBuffer = new StringBuffer();
        String temp = null;
        for (int i = 0; i < bytes.length; i++) {
            temp = Integer.toHexString(bytes[i] & 0xFF);
            if (temp.length() == 1) {
                //1得到一位的进行补0操作
                stringBuffer.append("0");
            }
            stringBuffer.append(temp);
        }
        return stringBuffer.toString();
    }

    /**
     * 获取16进制随机数
     *
     * @param len
     * @return
     */
    public static String randomHexString(int len) {
        try {
            StringBuffer result = new StringBuffer();
            for (int i = 0; i < len; i++) {
                result.append(Integer.toHexString(new Random().nextInt(16)));
            }
            return result.toString();

        } catch (Exception e) {
            logger.error(e.getMessage(), e);

        }
        return null;
    }


    /**
     * @param min 最小值
     * @param max 最大数
     * @Description 生产指定范围的随机数
     * @author zhang bo
     * @date 18-10-28
     * @version 1.0
     */
    public static int getRandomNumberInRange(int min, int max) {
        Random r = new Random();
        return r.ints(min, (max + 1)).findFirst().getAsInt();
    }


    /**
     * 字符串转换为16进制字符串
     *
     * @param s
     * @return
     */
    public static String stringToHexString(String s) {
        String str = "";
        for (int i = 0; i < s.length(); i++) {
            int ch = (int) s.charAt(i);
            String s4 = Integer.toHexString(ch);
            str = str + s4;
        }
        return str;
    }
}
 