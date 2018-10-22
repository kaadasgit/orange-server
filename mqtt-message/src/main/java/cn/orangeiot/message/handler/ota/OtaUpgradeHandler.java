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
import scala.util.parsing.json.JSONArray;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
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
        logger.debug("==UPgradeProcess==params -> {}", message.body());

        //獲取升級數據
        vertx.eventBus().send(OtaAddr.class.getName() + OTA_SELECT_DATA, message.body(), (AsyncResult<Message<JsonArray>> rs) -> {
            //0:強制升级 1用户确认升级 //2定时强制升级
            if (Objects.nonNull(rs.result()) && Objects.nonNull(rs.result().body())) {
                switch (message.body().getInteger("type")) {
                    case 0://强制升级
                        if (rs.result().body().size() > 0)
                            sendGatewayMSG(message, rs);
                        break;
                    case 1://用户确认升级
                        if (rs.result().body().size() > 0)
                            sendUserMsg(message, rs);
                        break;
                    case 2://通过app渠道升级
                        if (rs.result().body().size() > 0)
                            sendAppMsg(message, rs);
                        break;
                    default:
                        logger.warn("==OtaUpgradeHandler=UpgradeProcess params type -> {}", message.body().getInteger("type"));
                        break;
                }
            } else {
                logger.warn("device list size is 0 , params -> {}" + message.body());
            }
        });
    }


    /**
     * @Description app渠道升级
     * @author zhang bo
     * @date 18-7-4
     * @version 1.0
     */
    public void sendAppMsg(Message<JsonObject> message, AsyncResult<Message<JsonArray>> rs) {
        Map<Object, List<Object>> list = rs.result().body().stream().collect(Collectors.groupingBy(e ->
                new JsonObject(e.toString()).remove("uid"), Collectors.toList()
        ));
        list.forEach((k, v) -> {
            List<JsonObject> devList = v.stream().map(e -> {
                JsonObject map = new JsonObject(e.toString());
                map.remove("uid");
                return map;
            }).collect(Collectors.toList());
            JsonObject pushJsonObject = new JsonObject().put("func", "otaBlueUpgrade").put("gwId", "EMPTY")
                    .put("deviceId", "EMPTY")
                    .put("timestamp", System.currentTimeMillis()).put("msgId", 00001L).put("userId", k)
                    .put("params", new JsonObject().put("modelCode", message.body().getString("modelCode"))
                            .put("childCode", message.body().getString("childCode")).put("fileUrl"
                                    , message.body().getString("filePathUrl")).put("SW"
                                    , message.body().getString("SW")).put("deviceList"
                                    , new JsonArray(devList))
                            .put("fileMd5", message.body().getString("fileMd5")).put("fileLen"
                                    , message.body().getInteger("fileLen"))
                            .put("otaType", 1));

            DeliveryOptions deliveryOptions = new DeliveryOptions().addHeader("uid"
                    , "app:" + k).addHeader("qos", "1")
                    .addHeader("topic", MessageAddr.SEND_USER_REPLAY.replace("clientId", k.toString()));
            vertx.eventBus().send(MessageAddr.class.getName() + SEND_UPGRADE_MSG, pushJsonObject
                    , deliveryOptions);

            //记录信息
            JsonObject final_JsonObject = new JsonObject(pushJsonObject.toString());
            recordOTA(final_JsonObject.put("OTAOrderNo", message.body().getString("OTAOrderNo")));
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
        String modelDevice = "SN";
        if (message.body().getInteger("type") == 1)//用户确认升级
            modelDevice = "deviceSN";

        if (message.body().getInteger("modelType") == 2) {//掛載設備
            rs.result().body().stream().map(e -> {
                JsonObject jsonObject = new JsonObject(e.toString());
                JsonArray matIds = new JsonArray(rs.result().headers().get("matchIds"));
                List<String> devIds = jsonObject.getJsonArray("deviceList")
                        .stream().map(ids -> {
                            JsonObject dataJsonObject = new JsonObject(ids.toString());
                            if ((dataJsonObject.getString("event_str").equals("online")
                                    || dataJsonObject.getString("event_str").equals("offline")) && matIds.contains(dataJsonObject.getString("deviceId")))
                                return dataJsonObject.getString("deviceId");
                            else
                                return null;
                        }).collect(Collectors.toList());
                List<String> newDevIds = devIds.stream().filter(id -> id != null).collect(Collectors.toList());
                if (newDevIds.size() > 0) {
                    return new JsonObject().put("func", "otaApprovate").put("gwId", jsonObject.getString("deviceSN"))
                            .put("deviceId", jsonObject.getString("deviceSN"))
                            .put("timestamp", System.currentTimeMillis()).put("msgId", 00001L).put("userId", jsonObject.getString("adminuid"))
                            .put("params", new JsonObject().put("modelCode", message.body().getString("modelCode"))
                                    .put("childCode", message.body().getString("childCode")).put("fileUrl"
                                            , message.body().getString("filePathUrl")).put("SW"
                                            , message.body().getString("SW")).put("deviceList"
                                            , new JsonArray(newDevIds))
                                    .put("fileMd5", message.body().getString("fileMd5")).put("fileLen"
                                            , message.body().getInteger("fileLen")))
                            .put("otaType", 2);//掛載設備
                } else {
                    return null;
                }
            }).forEach(e -> {
                if (Objects.nonNull(e)) {
                    DeliveryOptions deliveryOptions = new DeliveryOptions().addHeader("uid"
                            , "app:" + e.getString("userId")).addHeader("qos", "1")
                            .addHeader("topic", MessageAddr.SEND_USER_REPLAY.replace("clientId", e.getString("userId")));
                    vertx.eventBus().send(MessageAddr.class.getName() + SEND_UPGRADE_MSG, e
                            , deliveryOptions);
                }
            });
        } else {//網關升級
            String finalModelDevice = modelDevice;
            rs.result().body().stream().map(e -> {
                JsonObject jsonObject = new JsonObject(e.toString());
                return new JsonObject().put("func", "otaApprovate").put("gwId", jsonObject.getString(finalModelDevice))
                        .put("deviceId", jsonObject.getString(finalModelDevice))
                        .put("timestamp", System.currentTimeMillis()).put("msgId", 00001L).put("userId", jsonObject.getString("adminuid"))
                        .put("params", new JsonObject().put("modelCode", message.body().getString("modelCode"))
                                .put("childCode", message.body().getString("childCode")).put("fileUrl"
                                        , message.body().getString("filePathUrl")).put("SW"
                                        , message.body().getString("SW")).put("deviceList"
                                        , new JsonArray().add(jsonObject.getString(finalModelDevice)))
                                .put("fileMd5", message.body().getString("fileMd5")).put("fileLen"
                                        , message.body().getInteger("fileLen"))
                                .put("otaType", 1));//网关升级
            }).forEach(e -> {
                DeliveryOptions deliveryOptions = new DeliveryOptions().addHeader("uid"
                        , "app:" + e.getString("userId")).addHeader("qos", "1")
                        .addHeader("topic", MessageAddr.SEND_USER_REPLAY.replace("clientId", e.getString("userId")));
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
                JsonArray matIds = new JsonArray(rs.result().headers().get("matchIds"));
                List<String> devIds = jsonObject.getJsonArray("deviceList")
                        .stream().map(ids -> {
                            JsonObject dataJsonObject = new JsonObject(ids.toString());
                            if ((dataJsonObject.getString("event_str").equals("online")
                                    || dataJsonObject.getString("event_str").equals("offline")) && matIds.contains(dataJsonObject.getString("deviceId")))
                                return dataJsonObject.getString("deviceId");
                            else
                                return null;
                        }).collect(Collectors.toList());
                List<String> newDevIds = devIds.stream().filter(id -> id != null).collect(Collectors.toList());
                if (newDevIds.size() > 0) {
                    return new JsonObject().put("func", "otaNotify").put("gwId", jsonObject.getString("deviceSN"))
                            .put("deviceId", jsonObject.getString("deviceSN"))
                            .put("timestamp", System.currentTimeMillis()).put("msgId", 00001L).put("userId", jsonObject.getString("adminuid"))
                            .put("params", new JsonObject().put("modelCode", message.body().getString("modelCode"))
                                    .put("childCode", message.body().getString("childCode")).put("fileUrl"
                                            , message.body().getString("filePathUrl")).put("SW"
                                            , message.body().getString("SW")).put("deviceList"
                                            , new JsonArray(newDevIds))
                                    .put("fileMd5", message.body().getString("fileMd5")).put("fileLen"
                                            , message.body().getInteger("fileLen")));
                } else {
                    return null;
                }
            }).forEach(e -> {
                if (Objects.nonNull(e)) {
                    DeliveryOptions deliveryOptions = new DeliveryOptions().addHeader("uid"
                            , "gw:" + e.getString("gwId")).addHeader("qos", "1")
                            .addHeader("topic", MessageAddr.SEND_GATEWAY_REPLAY.replace("gwId", e.getString("gwId")));
                    vertx.eventBus().send(MessageAddr.class.getName() + SEND_UPGRADE_MSG, e
                            , deliveryOptions);

                    JsonObject final_JsonObject = new JsonObject(e.toString());
                    recordOTA(final_JsonObject.put("OTAOrderNo", message.body().getString("OTAOrderNo")));
                }
            });
        } else {//網關升級
//            rs.result().body().stream().map(e -> {
//                JsonObject jsonObject = new JsonObject(e.toString());
//                return new JsonObject().put("func", "otaNotify").put("gwId", jsonObject.getString("SN"))
//                        .put("deviceId", jsonObject.getString("SN"))
//                        .put("timestamp", System.currentTimeMillis()).put("msgId", 00001L).put("userId", "SYS")
//                        .put("params", new JsonObject().put("modelCode", message.body().getString("modelCode"))
//                                .put("childCode", message.body().getString("childCode")).put("fileUrl"
//                                        , message.body().getString("filePathUrl")).put("SW"
//                                        , message.body().getString("SW")).put("deviceList"
//                                        , new JsonArray().add(jsonObject.getString("SN")))
//                                .put("fileMd5", message.body().getString("fileMd5")).put("fileLen"
//                                        , message.body().getInteger("fileLen")));
//            }).forEach(e -> {
//                DeliveryOptions deliveryOptions = new DeliveryOptions().addHeader("uid"
//                        , "gw:" + e.getString("gwId")).addHeader("qos", "1")
//                        .addHeader("topic", MessageAddr.SEND_GATEWAY_REPLAY.replace("gwId", e.getString("gwId")));
//                vertx.eventBus().send(MessageAddr.class.getName() + SEND_UPGRADE_MSG, e
//                        , deliveryOptions);
//
//                JsonObject final_JsonObject = new JsonObject(e.toString());
//                recordOTA(final_JsonObject.put("OTAOrderNo", message.body().getString("OTAOrderNo")));
//            });
            if (rs.result().body().size() > 0) {
                DeliveryOptions deliveryOptions = new DeliveryOptions().addHeader("qos", "1").setSendTimeout(15000);
                JsonObject jsonObject = new JsonObject().put("ids", rs.result().body()).put("func", "otaNotify")
                        .put("timestamp", System.currentTimeMillis()).put("msgId", 00001L).put("userId", "SYS")
                        .put("params", new JsonObject().put("modelCode", message.body().getString("modelCode"))
                                .put("childCode", message.body().getString("childCode")).put("fileUrl"
                                        , message.body().getString("filePathUrl")).put("SW"
                                        , message.body().getString("SW"))
                                .put("fileMd5", message.body().getString("fileMd5")).put("fileLen"
                                        , message.body().getInteger("fileLen")));
                vertx.eventBus().send(MessageAddr.class.getName() + SEND_VERSION_UPGRADE_MSG, jsonObject
                        , deliveryOptions);
            }
        }
    }


    /**
     * @Description 记录ota推送记录
     * @author zhang bo
     * @date 18-4-28
     * @version 1.0
     */
    public void recordOTA(JsonObject jsonObject) {
        //记录OTA审批日志
        vertx.eventBus().send(OtaAddr.class.getName() + OTA_APPROVATE_RECORD, new JsonObject()
                .put("uid", jsonObject.getString("userId")).put("deviceList", jsonObject.getJsonObject("params")
                        .getJsonArray("deviceList"))
                .put("type", Objects.nonNull(jsonObject.getJsonObject("params").getInteger("type"))
                        ? jsonObject.getJsonObject("params").getInteger("type") : 1)//默认升级
                .put("fileUrl", jsonObject.getJsonObject("params").getString("fileUrl"))
                .put("SW", jsonObject.getJsonObject("params").getString("SW"))
                .put("time", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .put("OTAOrderNo", jsonObject.getString("OTAOrderNo")));
    }

}
