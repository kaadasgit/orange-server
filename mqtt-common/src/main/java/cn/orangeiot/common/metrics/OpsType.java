package cn.orangeiot.common.metrics;

/**
 * @author : baijun
 * @date : 2019-01-18
 * @description : 动作类型
 */
public enum OpsType {

    /**
     * MQTT 连接数，记录上线，掉线日志
     */
    CONNECTION_COUNT("connection count");

    private String desc;

    private OpsType(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }
}
