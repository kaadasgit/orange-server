package cn.orangeiot.common.utils;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import org.apache.commons.lang3.StringUtils;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2017-12-28
 */
public class SHA256 {

    /**
     * SHA256加密
     *
     * @param str 加密后的报文
     * @return
     */
    public static void getSHA256Str(String str, Handler<AsyncResult<String>> handler) {
        MessageDigest messageDigest;
        if (StringUtils.isNotBlank(str)) {
            try {
                messageDigest = MessageDigest.getInstance("SHA-256");
                messageDigest.update(str.getBytes("UTF-8"));
                byte2Hex(messageDigest.digest(), rs -> {
                    if (rs.failed())
                        handler.handle(Future.failedFuture("SHA256==getSHA256Str==params Digest is fail"));
                    else
                        handler.handle(Future.succeededFuture(rs.result()));
                });
            } catch (Exception e) {
                e.getCause().getStackTrace();
                handler.handle(Future.failedFuture(e.getCause()));
            }
        } else {
            handler.handle(Future.failedFuture("SHA256==getSHA256Str==params is not null"));
        }
    }

    /**
     * 将byte转为16进制
     *
     * @param bytes
     * @return
     */
    @SuppressWarnings("Duplicates")
    private static void byte2Hex(byte[] bytes, Handler<AsyncResult<String>> handler) {
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
        handler.handle(Future.succeededFuture(stringBuffer.toString()));
    }


}
