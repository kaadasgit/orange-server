 package cn.orangeiot.common.utils;

 /**
  *  @author   :   luayuxiang
  *  @fileName :   com.Kaadas.KdsCreateRandom.java
  *
  *
  *
  *     date   |   author   |   version   |   revise   |
  *  2017年3月28日 |   windows   |     1.0     |            |
  *
  *  @describe :
  *
  *  ALL RIGHTS RESERVED,COPYRIGHT(C) Kaadas 2017
 */

public class KdsCreateRandom {
	/**
	* 创建指定数量的随机字符串
	* @param length
	* @return
	*/
	public static String createRandom( int length){
	 String retStr = "";
	 String strTable =  "1234567890" ;
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

}
 