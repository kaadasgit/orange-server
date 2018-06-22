 package cn.orangeiot.common.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

 /**
   *  @author   :   luayuxiang
   *  @fileName :   com.Kaadas.CreateMD5.java
   *
   *
   *
   *     date   |   author   |   version   |   revise   |
   *  2017年4月12日 |   windows   |     1.0     |            |
   *
   *  @describe : MD5 工具类 用于用户密码非明文存储
   *
   *  ALL RIGHTS RESERVED,COPYRIGHT(C) Kaadas 2017
  */

 public class KdsCreateMD5 {

     private static Logger logger = LogManager.getLogger(KdsCreateMD5.class);

     //静态方法，便于作为工具类
     public static String getMd5(String plainText) {
         try {
             MessageDigest md = MessageDigest.getInstance("MD5");
             md.update(plainText.getBytes());
             byte b[] = md.digest();

             int i;

             StringBuffer buf = new StringBuffer("");
             for (int offset = 0; offset < b.length; offset++) {
                 i = b[offset];
                 if (i < 0)
                     i += 256;
                 if (i < 16)
                     buf.append("0");
                 buf.append(Integer.toHexString(i));
             }
             //32位加密
             return buf.toString();
             // 16位的加密
             //return buf.toString().substring(8, 24);
         } catch (NoSuchAlgorithmException e) {
             logger.error(e.getMessage(), e);
             return null;
         }

     }
 }
 