package cn.orangeiot;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-04-19
 */
public class TestLoader {

    public static void main(String[] args) {
        BufferedInputStream in=new BufferedInputStream(TestLoader.class.getResourceAsStream("/httpServerStart.class"));
        try {
            byte[] bytes= new byte[in.available()];
            StringBuffer result = new StringBuffer();
            String hex;
            for (int i = 0; i < bytes.length; i++) {
                hex = Integer.toHexString(bytes[i] & 0xFF);
                if (hex.length() == 1) {
                    hex = '0' + hex;
                }
                result.append(hex.toUpperCase());
            }
            System.out.println(result.toString());

        } catch (IOException e) {
//            logger.error(e.getMessage(), e);
        }


    }
}
