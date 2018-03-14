package cn.orangeiot.common.constant;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-03-09
 */
public enum MediaTypeEnum {
    VIDEO, AUDIO;

    private String value;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
