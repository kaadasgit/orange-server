package cn.orangeiot.utils;


import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2017-11-27
 */
public class GUID
{

    public String valueBeforeMD5;
    public String valueAfterMD5;
    private static Random myRand;
    private static SecureRandom mySecureRand;
    private static String s_id;

    public GUID()
    {
        valueBeforeMD5 = "";
        valueAfterMD5 = "";
        getRandomGUID(false);
    }

    public GUID(boolean secure)
    {
        valueBeforeMD5 = "";
        valueAfterMD5 = "";
        getRandomGUID(secure);
    }

    private void getRandomGUID(boolean secure)
    {
        MessageDigest md5 = null;
        StringBuffer sbValueBeforeMD5 = new StringBuffer();
        try
        {
            md5 = MessageDigest.getInstance("MD5");
        }
        catch(NoSuchAlgorithmException e)
        {
            System.out.println("Error: " + e);
        }
        try
        {
            long time = System.currentTimeMillis();
            long rand = 0L;
            if(secure)
                rand = mySecureRand.nextLong();
            else
                rand = myRand.nextLong();
            sbValueBeforeMD5.append(s_id);
            sbValueBeforeMD5.append(":");
            sbValueBeforeMD5.append(Long.toString(time));
            sbValueBeforeMD5.append(":");
            sbValueBeforeMD5.append(Long.toString(rand));
            valueBeforeMD5 = sbValueBeforeMD5.toString();
            md5.update(valueBeforeMD5.getBytes());
            byte array[] = md5.digest();
            StringBuffer sb = new StringBuffer();
            for(int j = 0; j < array.length; j++)
            {
                int b = array[j] & 0xff;
                if(b < 16)
                    sb.append('0');
                sb.append(Integer.toHexString(b));
            }

            valueAfterMD5 = sb.toString();
        }
        catch(Exception e)
        {
            System.out.println("Error:" + e);
        }
    }

    public String toString()
    {
        String raw = valueAfterMD5.toUpperCase();
        StringBuffer sb = new StringBuffer();
        sb.append(raw.substring(0, 8));
        sb.append("-");
        sb.append(raw.substring(8, 12));
        sb.append("-");
        sb.append(raw.substring(12, 16));
        sb.append("-");
        sb.append(raw.substring(16, 20));
        sb.append("-");
        sb.append(raw.substring(20));
        return sb.toString();
    }


    public static String SHA(String s)
    {
        String s1 = "";
        if(s.trim() == null)
            return "null";

        MessageDigest messagedigest;
        try {
            messagedigest = MessageDigest.getInstance("SHA");
            messagedigest.update(s.getBytes());
            byte abyte[] = messagedigest.digest();
            //	BigInteger a = new BigInteger(abyte);
            //	s1 = a.toString(10);
            for (int i = 0; i < abyte.length; i++)
                s1 += abyte[i];
        } catch (NoSuchAlgorithmException e) {e.printStackTrace();}

        return s1;
    }

    public static String MD5(String text) {
        String Digits="0123456789abcdef";
        String md5_str = "";
        try{
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(text.getBytes());
            byte[] array = md5.digest();
            for(int i = 0;i<array.length;i++)
            {
                int val = array[i];
                if(val < 0)val += 256;
                int h = val / 16;
                int l = val % 16;

                md5_str += Digits.substring(h, h + 1) + Digits.substring(l, l + 1);
            }
        }catch(Exception e){e.printStackTrace();}
        return md5_str;
    }

    public static String mpMD5(String text) {
        String md5_str = "";
        if (text == null)
            text = "";
        if (text.length() > 0) {
            try {
                MessageDigest md5 = MessageDigest.getInstance("MD5");
                md5.update(text.getBytes());
                byte[] array = md5.digest();
                for (int i = 0; i < array.length; i++)
                    md5_str += array[i];
                md5_str = md5_str.replaceAll("-", "").substring(0, 16);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return md5_str;

    }

    static
    {
        mySecureRand = new SecureRandom();
        long secureInitializer = mySecureRand.nextLong();
        myRand = new Random(secureInitializer);
        try
        {
            s_id = InetAddress.getLocalHost().toString();
        }
        catch(UnknownHostException e)
        {
            e.printStackTrace();
        }
    }
}
