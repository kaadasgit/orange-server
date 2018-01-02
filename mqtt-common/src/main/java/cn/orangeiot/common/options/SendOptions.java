package cn.orangeiot.common.options;


import io.vertx.core.eventbus.DeliveryOptions;

/**
 * @author zhang bo
 * @version 1.0
 * @Description 发送的配置
 * @date 2018-01-02
 */
public class SendOptions {

    private static final DeliveryOptions single = new DeliveryOptions().setSendTimeout(2000);//单位毫秒

    public static DeliveryOptions getInstance() {
        return single;
    }
}