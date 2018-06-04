package cn.orangeiot.message.handler.ota;

import cn.orangeiot.reg.EventbusAddr;
import cn.orangeiot.reg.message.MessageAddr;
import cn.orangeiot.reg.ota.OtaAddr;
import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-04-04
 */
public class OtaUpgradeHandler implements EventbusAddr {

    private static Logger logger = LogManager.getLogger(OtaUpgradeHandler.class);

    private Vertx vertx;

    private JsonObject config;

    public OtaUpgradeHandler(Vertx vertx, JsonObject config) {
        this.config = config;
        this.vertx = vertx;
    }


    /**
     * @Description OTA升级处理
     * @author zhang bo
     * @date 18-4-4
     * @version 1.0
     */
    public void UpgradeProcess(Message<JsonObject> message) {
        logger.info("==UPgradeProcess==params -> {}", message.body());

        //獲取升級數據
        vertx.eventBus().send(OtaAddr.class.getName() + OTA_SELECT_DATA, message.body(), (AsyncResult<Message<JsonArray>> rs) -> {
            //0:強制升级 1用户确认升级 //2定时强制升级
            switch (message.body().getInteger("type")) {
                case 0:
                    if (Objects.nonNull(rs.result().body().size() > 0))
                        sendGatewayMSG(message, rs);
                    break;
                case 1:
                    if (Objects.nonNull(rs.result().body().size() > 0))
                        sendUserMsg(message, rs);
                    break;
                case 2:
                    break;
                default:
                    logger.warn("==OtaUpgradeHandler=UpgradeProcess params type -> {}", message.body().getInteger("type"));
                    break;
            }
        });
    }


    /**
     * @Description 發送消息到用戶
     * @author zhang bo
     * @date 18-4-19
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void sendUserMsg(Message<JsonObject> message, AsyncResult<Message<JsonArray>> rs) {
        if (message.body().getInteger("modelType") == 2) {//掛載設備
            rs.result().body().stream().map(e -> {
                JsonObject jsonObject = new JsonObject(e.toString());
                List<String> devIds = jsonObject.getJsonArray("deviceList")
                        .stream().map(ids -> {
                            JsonObject dataJsonObject = new JsonObject(ids.toString());
                            if (dataJsonObject.getInteger("status").equals(1))
                                return dataJsonObject.getString("devid");
                            else
                                return null;
                        }).collect(Collectors.toList());
                devIds.remove(null);
                return new JsonObject().put("func", "otaApprovate").put("gwId", jsonObject.getString("deviceSN"))
                        .put("deviceId", jsonObject.getString("deviceSN"))
                        .put("timestamp", System.currentTimeMillis()).put("msgId", 00001L).put("userId", jsonObject.getString("adminuid"))
                        .put("params", new JsonObject().put("modelCode", message.body().getString("modelCode"))
                                .put("childCode", message.body().getString("childCode")).put("fileUrl"
                                        , message.body().getString("filePathUrl")).put("SW"
                                        , message.body().getString("SW")).put("deviceList"
                                        , new JsonArray(devIds))
                                .put("fileMd5", message.body().getString("fileMd5")).put("fileLen"
                                        , message.body().getInteger("fileLen")))
                                .put("otaType", 2);//掛載設備
            }).forEach(e -> {
                DeliveryOptions deliveryOptions = new DeliveryOptions().addHeader("uid"
                        ,"app:" + e.getString("userId")).addHeader("qos", "1")
                        .addHeader("topic",MessageAddr.SEND_USER_REPLAY.replace("clientId", e.getString("userId")));
                vertx.eventBus().send(MessageAddr.class.getName() + SEND_UPGRADE_MSG, e
                        , deliveryOptions);
            });
        } else {//網關升級
            rs.result().body().stream().map(e -> {
                JsonObject jsonObject = new JsonObject(e.toString());
                return new JsonObject().put("func", "otaApprovate").put("gwId", jsonObject.getString("deviceSN"))
                        .put("deviceId", jsonObject.getString("deviceSN"))
                        .put("timestamp", System.currentTimeMillis()).put("msgId", 00001L).put("userId", jsonObject.getString("adminuid"))
                        .put("params", new JsonObject().put("modelCode", message.body().getString("modelCode"))
                                .put("childCode", message.body().getString("childCode")).put("fileUrl"
                                        , message.body().getString("filePathUrl")).put("SW"
                                        , message.body().getString("SW")).put("deviceList"
                                        , new JsonArray().add(jsonObject.getString("deviceSN")))
                                .put("fileMd5", message.body().getString("fileMd5")).put("fileLen"
                                        , message.body().getInteger("fileLen"))
                                .put("otaType", 1));//网关升级
            }).forEach(e -> {
                DeliveryOptions deliveryOptions = new DeliveryOptions().addHeader("uid"
                        , "app:" + e.getString("userId")).addHeader("qos", "1")
                        .addHeader("topic",MessageAddr.SEND_USER_REPLAY.replace("clientId", e.getString("userId")));;
                vertx.eventBus().send(MessageAddr.class.getName() + SEND_UPGRADE_MSG, e
                        , deliveryOptions);
            });
        }
    }


    /**
     * @Description 發送消息到網關
     * @author zhang bo
     * @date 18-4-17
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void sendGatewayMSG(Message<JsonObject> message, AsyncResult<Message<JsonArray>> rs) {
        if (message.body().getInteger("modelType") == 2) {//掛載設備
            rs.result().body().stream().map(e -> {
                JsonObject jsonObject = new JsonObject(e.toString());
                List<String> devIds = jsonObject.getJsonArray("deviceList")
                        .stream().map(ids -> {
                            JsonObject dataJsonObject = new JsonObject(ids.toString());
                            if (dataJsonObject.getInteger("status").equals(1))
                                return dataJsonObject.getString("devid");
                            else
                                return null;
                        }).collect(Collectors.toList());
                devIds.remove(null);
                return new JsonObject().put("func", "otaNotify").put("gwId", jsonObject.getString("deviceSN"))
                        .put("deviceId", jsonObject.getString("deviceSN"))
                        .put("timestamp", System.currentTimeMillis()).put("msgId", 00001L).put("userId", jsonObject.getString("adminuid"))
                        .put("params", new JsonObject().put("modelCode", message.body().getString("modelCode"))
                        .put("childCode", message.body().getString("childCode")).put("fileUrl"
                                , message.body().getString("filePathUrl")).put("SW"
                                , message.body().getString("SW")).put("deviceList"
                                , new JsonArray(devIds))
                        .put("fileMd5", message.body().getString("fileMd5")).put("fileLen"
                                , message.body().getInteger("fileLen")));
            }).forEach(e -> {
                DeliveryOptions deliveryOptions = new DeliveryOptions().addHeader("uid"
                        , "gw:"+e.getString("gwId")).addHeader("qos", "1")
                        .addHeader("topic",MessageAddr.SEND_GATEWAY_REPLAY.replace("gwId", e.getString("gwId")));
                vertx.eventBus().send(MessageAddr.class.getName() + SEND_UPGRADE_MSG, e
                        , deliveryOptions);
                recordOTA(e);
            });
        } else {//網關升級
            rs.result().body().stream().map(e -> {
                JsonObject jsonObject = new JsonObject(e.toString());
                return new JsonObject().put("func", "otaNotify").put("gwId", jsonObject.getString("deviceSN"))
                        .put("deviceId", jsonObject.getString("deviceSN"))
                        .put("timestamp", System.currentTimeMillis()).put("msgId", 00001L).put("userId", jsonObject.getString("adminuid"))
                        .put("params", new JsonObject().put("modelCode", message.body().getString("modelCode"))
                        .put("childCode", message.body().getString("childCode")).put("fileUrl"
                                , message.body().getString("filePathUrl")).put("SW"
                                , message.body().getString("SW")).put("deviceList"
                                , new JsonArray().add(jsonObject.getString("deviceSN")))
                        .put("fileMd5", message.body().getString("fileMd5")).put("fileLen"
                                , message.body().getInteger("fileLen")));
            }).forEach(e -> {
                DeliveryOptions deliveryOptions = new DeliveryOptions().addHeader("uid"
                        , "gw:"+e.getString("gwId")).addHeader("qos", "1")
                        .addHeader("topic",MessageAddr.SEND_GATEWAY_REPLAY.replace("gwId", e.getString("gwId")));
                vertx.eventBus().send(MessageAddr.class.getName() + SEND_UPGRADE_MSG, e
                        , deliveryOptions);
                recordOTA(e);
            });
        }
    }


    /**
     * @Description  记录ota推送记录
     * @author zhang bo
     * @date 18-4-28
     * @version 1.0
     */
    public void recordOTA(JsonObject jsonObject){
        //记录OTA审批日志
        vertx.eventBus().send(OtaAddr.class.getName() + OTA_APPROVATE_RECORD, new JsonObject()
                .put("uid", jsonObject.getString("userId")).put("deviceList", jsonObject.getJsonObject("params")
                        .getJsonArray("deviceList"))
                .put("type", jsonObject.getJsonObject("params").getInteger("type"))
                .put("fileUrl", jsonObject.getJsonObject("params").getString("fileUrl"))
                .put("SW", jsonObject.getJsonObject("params").getString("SW"))
                .put("time", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
    }

}
