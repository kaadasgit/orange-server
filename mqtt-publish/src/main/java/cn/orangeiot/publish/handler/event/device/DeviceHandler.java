package cn.orangeiot.publish.handler.event.device;

import cn.orangeiot.common.genera.ErrorType;
import cn.orangeiot.common.options.SendOptions;
import cn.orangeiot.common.utils.DataType;
import cn.orangeiot.common.verify.VerifyParamsUtil;
import cn.orangeiot.publish.handler.event.EventHandler;
import cn.orangeiot.publish.model.ResultInfo;
import cn.orangeiot.reg.EventbusAddr;
import cn.orangeiot.reg.gateway.GatewayAddr;
import cn.orangeiot.reg.message.MessageAddr;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
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
public class DeviceHandler implements EventbusAddr {

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

    /**
     * @Description 設備刪除事件
     * @author zhang bo
     * @date 18-3-22
     * @version 1.0
     */
    public void devicedelete(JsonObject jsonObject) {
        vertx.eventBus().send(GatewayAddr.class.getName() + DEVICE_DELETE, jsonObject);
    }


    /**
     * @Description 獲取網關設備列表
     * @author zhang bo
     * @date 18-3-22
     * @version 1.0
     */
    public void getDeviceList(JsonObject jsonObject) {
        vertx.eventBus().send(GatewayAddr.class.getName() + GET_GW_DEVICE_List, jsonObject, SendOptions.getInstance(), rs -> {
            if (rs.failed()) {
                logger.error(rs.cause().getMessage(), rs);
            } else {
                DeliveryOptions deliveryOptions = new DeliveryOptions().addHeader("uid"
                        , "gw:" + jsonObject.getString("gwId")).addHeader("qos", "1")
                        .addHeader("topic", MessageAddr.SEND_GATEWAY_REPLAY.replace("gwId", jsonObject.getString("gwId")));

                vertx.eventBus().send(MessageAddr.class.getName() + SEND_UPGRADE_MSG, jsonObject
                                .put("returnCode", 200).put("returnData", rs.result().body())
                        , deliveryOptions);
            }
        });
    }


    /**
     * @Description 時間開鎖
     * @author zhang bo
     * @date 18-7-20
     * @version 1.0
     */
    public void openLock(JsonObject jsonObject) {
        vertx.eventBus().send(GatewayAddr.class.getName() + EVENT_OPEN_LOCK, jsonObject);
    }

}
