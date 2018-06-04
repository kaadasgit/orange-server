package cn.orangeiot.publish.handler.event.device;

import cn.orangeiot.publish.handler.event.EventHandler;
import cn.orangeiot.reg.gateway.GatewayAddr;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Objects;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-03-22
 */
public class DeviceHandler implements GatewayAddr {

    private static Logger logger = LogManager.getLogger(DeviceHandler.class);

    private Vertx vertx;

    private JsonObject jsonObject;

    public DeviceHandler(Vertx vertx, JsonObject jsonObject) {
        this.vertx = vertx;
        this.jsonObject = jsonObject;
    }


    /**
     * @Description 設備上報事件
     * @author zhang bo
     * @date 18-3-22
     * @version 1.0
     */
    public void deviceOnline(JsonObject jsonObject) {
        vertx.eventBus().send(GatewayAddr.class.getName() + DEVICE_ONLINE, jsonObject);
    }


    /**
     * @Description 設備下線事件
     * @author zhang bo
     * @date 18-3-22
     * @version 1.0
     */
    public void deviceOffline(JsonObject jsonObject) {
        vertx.eventBus().send(GatewayAddr.class.getName() + DEVICE_OFFLINE, jsonObject);
    }

}
