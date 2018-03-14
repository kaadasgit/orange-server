package cn.orangeiot;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-03-01
 */
public class Result {

    private String one = "";

    private String two = "";

    private String sum = "";

    public Result one(String str) {
        this.one = str;
        return this;
    }

    public Result two(String str) {
        this.two = str;
        return this;
    }

    public String sum() {
        return this.sum = one + two;
    }
}
