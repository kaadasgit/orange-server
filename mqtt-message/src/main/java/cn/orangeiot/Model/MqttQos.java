package cn.orangeiot.Model;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2017-11-24
 */
public enum MqttQos {
    AT_MOST_ONCE(0),
    AT_LEAST_ONCE(1),
    EXACTLY_ONCE(2);

    private Integer value;

    MqttQos(Integer value) {
        this.value = value;
    }

    public Integer getValue() {
        return value;
    }

    public void setValue(Integer value) {
        this.value = value;
    }
}