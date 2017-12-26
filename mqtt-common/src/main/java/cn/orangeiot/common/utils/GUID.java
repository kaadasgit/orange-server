package cn.orangeiot.common.utils;

/*
 * 创建日期 2006-5-25，作者： 徐一清 、朱超
 *
 * 类功能：产生全球唯一码GUID
 * 
 */

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.server.UID;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.log4j.chainsaw.Main;
public class GUID implements Serializable{

    /**
     * simple GUID (Globally Unique ID) implementation.
     * A GUID is composed of two parts:
     * 1. The IP-Address of the local machine.
     * 2. A java.rmi.server.UID
     *
     * @author Thomas Mahler
     * @version $Id: GUID.java,v 1.1 2012/11/08 08:53:04 administrator Exp $
     */
	static final long serialVersionUID = -6163239155380515945L;    

	private static String localIPAddress;// holds the hostname of the local machine.

    private String guid;//String representation of the GUID

    static//compute the local IP-Address
    {
        try
        {
            localIPAddress = InetAddress.getLocalHost().getHostAddress();
        }
        catch (UnknownHostException e)
        {
            localIPAddress = "localhost";
        }
    }


    public GUID() {
        UID uid = new UID();
        StringBuffer buf = new StringBuffer();
        buf.append(localIPAddress);
        buf.append(":");
        buf.append(uid.toString());
        guid = buf.toString();

    }
    /**
     * public constructor.
     * The caller is responsible to feed a globally unique 
     * String into the theGuidString parameter
     * @param theGuidString a globally unique String
     */    
    public GUID(String theGuidString)
    {
    	guid = theGuidString;	
    }

    public String toString()//returns the String representation of the GUID
    {
        return guid;
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

    public static String MD5(String text)
    {
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
    public static String sno()
    {
    	UID uid = new UID();
     	String sno = "" + uid.hashCode();
     	sno = sno.replaceAll("-", "");
    	return sno;
    }
    //从0到limit中获取一个随机数
    public static int int_random(int limit)
    {
    	int value = (int)(float)(Math.random() * 100000 );
    	
    	return value % limit;
    }
    /**
	 * @see Object#equals(Object)
	 */
    public boolean equals(Object obj)
    {
    	if (obj instanceof GUID)
    	{
    		if (guid.equals(((GUID) obj).guid))
    		{
    			return true;	
    		}	
    	}
        return false;
    }

    /**
     * @see Object#hashCode()
     */
    public int hashCode()
    {
        return guid.hashCode();
    }
    
    public static void main(String[] args) {
		System.out.println(GUID.MD5("123456"));
	}

}
