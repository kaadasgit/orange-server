package cn.orangeiot.common.utils;

import java.nio.Buffer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-07-01
 */
public class CreateOTAOrderNoUtils {


    private static String OTA_LOGO = "OTA";//OTA標識

    /**
     * @Description 生成ota訂單號S
     * @author zhang bo
     * @date 18-7-1
     * @version 1.0
     */
    public static String getOTAOrderNo() {
        StringBuffer buffer = new StringBuffer();
        String machine = OTA_LOGO;
        String random = KdsCreateRandom.createRandom(4);
        buffer.append(machine).append(random);
        return byte2Hex(buffer.toString().getBytes());
    }




    /**
     *
     * @param bytes
     * @return
     */
    @SuppressWarnings("Duplicates")
    private static String byte2Hex(byte[] bytes) {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append((System.currentTimeMillis() / 1000));
        String temp;
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


    public static void main(String[] args) {
        System.out.println(getOTAOrderNo());
        System.out.println(getOTAOrderNo().length());
    }
}
