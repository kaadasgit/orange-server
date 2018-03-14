package cn.orangeiot;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-03-01
 */
public class Test {

    public static void main(String[] args) {
        String c = new Result().one("1").two("2").sum();
        System.out.println(c);
    }

}
