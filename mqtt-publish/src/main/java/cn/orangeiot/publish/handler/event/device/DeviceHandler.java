package cn.orangeiot.publish.handler.event.device;

import cn.orangeiot.common.genera.ErrorType;
import cn.orangeiot.common.options.SendOptions;
import cn.orangeiot.common.utils.DataType;
import cn.orangeiot.common.utils.StatusCode;
import cn.orangeiot.common.verify.VerifyParamsUtil;
import cn.orangeiot.publish.handler.event.EventHandler;
import cn.orangeiot.publish.model.ResultInfo;
import cn.orangeiot.reg.EventbusAddr;
import cn.orangeiot.reg.gateway.GatewayAddr;
import cn.orangeiot.reg.memenet.MemenetAddr;
import cn.orangeiot.reg.message.MessageAddr;
import io.vertx.core.*;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
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


    /**
     * @Description 重置設備
     * @author zhang bo
     * @date 18-8-31
     * @version 1.0
     */
    public void resetDevice(JsonObject jsonObject, MultiMap headers, JsonObject conf) {
        //配置数据
        JsonObject dataObject = new JsonObject()
                .put("userId", jsonObject.getString("userId")).put("deviceId", "EMPTY").put("gwId", jsonObject.getString("gwId"))
                .put("func", "cleanDevAll").put("msgId", 1).put("timestamp", System.currentTimeMillis());
        DeliveryOptions deliveryOptions = SendOptions.getInstance().addHeader("topic", conf.getString("repeat_message").replace("gwId", jsonObject.getString("gwId")))
                .addHeader("qos", headers.get("qos")).addHeader("messageId", headers.get("messageId"))
                .addHeader("uid", jsonObject.getString("gwId")).addHeader("redict", "1");

        vertx.eventBus().send(GatewayAddr.class.getName() + RESET_DEVICE, jsonObject,
                SendOptions.getInstance(), (AsyncResult<Message<JsonObject>> rs) -> {
                    if (rs.failed()) {
                        vertx.eventBus().send(MessageAddr.class.getName() + SEND_UPGRADE_MSG
                                , dataObject.put("returnCode", StatusCode.SERVER_ERROR), deliveryOptions);
                    } else {
                        if (Objects.nonNull(rs.result().body())) {//数据是否存在
                            //同步第三方信息
                            vertx.eventBus().send(MemenetAddr.class.getName() + RELIEVE_DEVICE_USER, rs.result().body()
                                            .put("uid",rs.result().body().getString("adminuid"))
                                            .put("devuuid",jsonObject.getString("gwId")).put("mult", rs.result().headers().get("mult"))
                                    , SendOptions.getInstance());

                            vertx.eventBus().send(MessageAddr.class.getName() + SEND_UPGRADE_MSG
                                    , dataObject.put("returnCode", StatusCode.SUCCESSS), deliveryOptions);
                        } else {
                            vertx.eventBus().send(MessageAddr.class.getName() + SEND_UPGRADE_MSG
                                    , dataObject.put("returnCode", StatusCode.Not_FOUND), deliveryOptions);
                        }
                    }
                });
    }

}
