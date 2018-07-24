package cn.orangeiot.common.utils;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2017-12-08
 */
public class EscapeUtils {

    /**
     * html转义
     * @param text
     * @return转码
     */
    public static String escapeHtml(String text) {
        if (text == null || text.isEmpty()) return text;
        StringBuffer sb = new StringBuffer();
        int n = text.length();
        for (int i = 0; i < n; i++) {
            char ch = text.charAt(i);
            switch (ch) {
                case '<':  sb.append("&lt;"); break;
                case '>':  sb.append("&gt;"); break;
                case '&':  sb.append("&amp;"); break;
//                case '"':  sb.append("&quot;"); break;
                case ' ':  sb.append("&nbsp;"); break;
                case '\'': sb.append("&#39;");break;
//                case '\n': sb.append("");break;
//                case ' ': sb.append("");break;
                default :  sb.append(ch); break;
            }
        }
        return sb.toString();
    }

    /**
     * String转义html
     * @param s
     * @return
     */
    public static String[] toHtmlCode(String[] s){
        for(int i=0;i<s.length;i++){
            String resultStr=escapeHtml(s[i]);
            s[i]=resultStr;
        }
        return s;
    }

    public static void main(String[] args){
        String text1=">>>阿萨达速度 撒打 算a<<< \"";
        String text2=">>>看看看看 卡  阿萨德<<< \"";
        String[] arr ={text1,text2};
        arr=toHtmlCode(arr);
        System.out.println(">>>>>"+arr[0]+">>>>"+arr[1]);
    }

}
