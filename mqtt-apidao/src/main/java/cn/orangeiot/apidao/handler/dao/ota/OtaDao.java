package cn.orangeiot.apidao.handler.dao.ota;

import cn.orangeiot.apidao.client.MongoClient;
import cn.orangeiot.common.constant.mongodb.*;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.FindOptions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-03-29
 */
public class OtaDao {

    private static Logger logger = LogManager.getLogger(OtaDao.class);


    /**
     * @Description 查詢產品類型
     * @author zhang bo
     * @date 18-3-29
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void selectModelType(Message<String> message) {
        logger.info("==selectModelType==params -> {}", message.body());

        MongoClient.client.runCommand("aggregate", new JsonObject().put("aggregate", KdsModelInfo.COLLECT_NAME)
                .put("pipeline", new JsonArray().add(new JsonObject().put("$group", new JsonObject().put("_id"
                        , new JsonObject().put(KdsModelInfo.MODEL_CODE, "$modelCode").put(KdsModelInfo.CHILD_CODE, "$childCode")
                                .put(KdsModelInfo.TIME, "$time")))).add(new JsonObject().put("$sort", new JsonObject().put("_id.time", 1)))), rs -> {
            if (rs.failed()) {
                logger.error(rs.cause().getMessage(), rs);
            } else {
                logger.info("selectModelType==mongo== result -> {}", rs.result());
                message.reply(rs.result());
            }
        });
    }


    /**
     * @Description 查詢時期範圍
     * @author zhang bo
     * @date 18-3-29
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void selectDateRange(Message<JsonObject> message) {
        logger.info("==selectDateRange==params -> {}", message.body());

        MongoClient.client.findWithOptions(KdsModelInfo.COLLECT_NAME, new JsonObject().put(KdsModelInfo.MODEL_CODE, message.body().getString("modelCode"))
                        .put(KdsModelInfo.CHILD_CODE, message.body().getString("childCode"))
                , new FindOptions().setFields(new JsonObject().put(KdsModelInfo.YEAR_CODE, 1).put(KdsModelInfo.WEEK_CODE, 1).put(KdsModelInfo._ID, 0))
                        .setSort(new JsonObject().put(KdsModelInfo.WEEK_CODE, 1)), rs -> {
                    if (rs.failed()) {
                        logger.error(rs.cause().getMessage(), rs);
                    } else {
                        logger.info("selectModelType==mongo== result -> {}", rs.result());
                        message.reply(new JsonArray(rs.result()));
                    }
                });
    }


    /**
     * @Description 查询編號範圍
     * @author zhang bo
     * @date 18-3-29
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void selectNumRange(Message<JsonObject> message) {
        logger.info("==selectNumRange==params -> {}", message.body());

        MongoClient.client.findWithOptions(KdsModelInfo.COLLECT_NAME, new JsonObject().put(KdsModelInfo.MODEL_CODE, message.body().getString("modelCode"))
                        .put(KdsModelInfo.CHILD_CODE, message.body().getString("childCode")).put(KdsModelInfo.YEAR_CODE, message.body().getString("yearCode"))
                        .put(KdsModelInfo.WEEK_CODE, message.body().getString("weekCode"))
                , new FindOptions().setFields(new JsonObject().put(KdsModelInfo.COUNT, 1).put(KdsModelInfo._ID, 0)), rs -> {
                    if (rs.failed()) {
                        logger.error(rs.cause().getMessage(), rs);
                    } else {
                        logger.info("selectModelType==mongo== result -> {}", rs.result());
                        int sum = 0;
                        if (rs.result().size() > 0) {
                            sum = rs.result().stream().mapToInt(e -> new JsonObject(e.toString()).getInteger("count")).sum();
                        }
                        message.reply(sum);
                    }
                });
    }


    /**
     * @Description 提交ota升级的数据
     * @author zhang bo
     * @date 18-4-2
     * @version 1.0
     */
    public void submitOTAUpgrade(Message<JsonObject> message) {
        logger.info("==submitOTAUpgrade==params -> {}", message.body());

        MongoClient.client.insert(KdsOtaUpgrade.COLLECT_NAME, message.body().put(KdsOtaUpgrade.TIME,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))), rs -> {
            if (rs.failed()) logger.error(rs.cause().getMessage(), rs);
        });

    }


    /**
     * @Description 獲取升級的設備
     * @author zhang bo
     * @date 18-4-11
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void getUpgradeDevice(Message<JsonObject> message) {
        logger.info("==getUpgradeDevice==params -> {}", message.body());

        JsonObject paramsJsonObject = new JsonObject().put(KdsProductInfoList.COLLECT_NAME, message.body().getString("modelCode"))
                .put(KdsProductInfoList.CHILD_CODE, message.body().getString("childCode"));

        //生產日期範圍
        if (!message.body().getString("yearCode").equals("ALL")) {
            paramsJsonObject.put(KdsProductInfoList.YEAR_CODE, message.body().getString("yearCode"));
            if (!message.body().getString("weekCode").equals("ALL"))
                paramsJsonObject.put(KdsProductInfoList.WEEK_CODE, message.body().getString("weekCode"));
        }

        String[] arrs;
        //區分範圍
        if (message.body().getString("range").indexOf("-") > 0) {
            arrs = message.body().getString("range").split("-");
            paramsJsonObject.put("position", new JsonObject().put("$gte", Integer.parseInt(arrs[0])).put("$lte"
                    , Integer.parseInt(arrs[1])));
        } else {
            arrs = message.body().getString("range").split(",");
            List<Integer> lists = Arrays.stream(arrs).map(e -> Integer.parseInt(e)).collect(Collectors.toList());
            paramsJsonObject.put("position", new JsonObject().put("$in", new JsonArray(lists)));
        }

        //獲取設備PN號
        MongoClient.client.findWithOptions(KdsProductInfoList.COLLECT_NAME, paramsJsonObject,
                new FindOptions().setFields(new JsonObject().put(KdsProductInfoList.SN, 1).put(KdsProductInfoList._ID, 0).put(KdsProductInfoList.MAC, 1)), rs -> {
                    if (rs.failed()) {
                        logger.error(rs.cause().getMessage(), rs);
                        message.reply(null);
                    } else {
                        JsonObject pnJsonObject = new JsonObject();
                        List<String> _ids = rs.result().stream().map(e -> new JsonObject(e.toString()).getString("SN"))
                                .collect(Collectors.toList());
                        switch (message.body().getInteger("modelType")) {
                            case 1://网关
                                getDeviceByGateway(pnJsonObject, message, _ids);
                                return;
                            case 2://挂载设备
                                pnJsonObject.put("deviceList.deviceId", new JsonObject().put("$in", new JsonArray(_ids)));
                                break;
                            case 3://蓝牙
                                getDeviceByApp(rs.result(), message);
                                return;
                            default:
                                logger.warn("modelType type is not params -> {}", message.body().getInteger("modelType"));
                                message.reply(null);
                                return;
                        }
                        MongoClient.client.findWithOptions(KdsGatewayDeviceList.COLLECT_NAME, pnJsonObject.put(KdsGatewayDeviceList.IS_ADMIN, 1)
                                , new FindOptions().setFields(new JsonObject().put("deviceList.deviceId", 1)
                                        .put(KdsGatewayDeviceList.DEVICE_SN, 1).put(KdsGatewayDeviceList._ID, 0).put("deviceList.event_str", 1)
                                        .put(KdsGatewayDeviceList.ADMIN_UID, 1)), as -> {
                                    if (as.failed()) {
                                        logger.error(as.cause().getMessage(), as);
                                        message.reply(null);
                                    } else {
                                        message.reply(new JsonArray(as.result())
                                                , new DeliveryOptions().addHeader("matchIds", new JsonArray(_ids).toString()));
                                    }
                                });
                    }
                });
    }


    /**
     * @Description 根据gateway获取相应设备
     * @author zhang bo
     * @date 18-7-16
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void getDeviceByGateway(JsonObject jsonObject, Message<JsonObject> message, List<String> ids) {
        if (message.body().getInteger("type") == 1) {//用户确认升级
            jsonObject.put(KdsGatewayDeviceList.DEVICE_SN, new JsonObject().put("$in", new JsonArray(ids)))
                    .put(KdsGatewayDeviceList.IS_ADMIN, 1);//管理员
            MongoClient.client.findWithOptions(KdsGatewayDeviceList.COLLECT_NAME, jsonObject
                    , new FindOptions().setFields(new JsonObject().put("deviceList.deviceId", 1)
                            .put(KdsGatewayDeviceList.DEVICE_SN, 1).put(KdsGatewayDeviceList._ID, 0).put("deviceList.event_str", 1)
                            .put(KdsGatewayDeviceList.ADMIN_UID, 1)), as -> {
                        if (as.failed()) {
                            logger.error(as.cause().getMessage(), as);
                            message.reply(null);
                        } else {
                            message.reply(new JsonArray(as.result()));
                        }
                    });
        } else {
//            jsonObject.put("SN", new JsonObject().put("$in", new JsonArray(ids)));
//            MongoClient.client.findWithOptions("kdsProductInfoList", jsonObject
//                    , new FindOptions().setFields(new JsonObject().put("SN", 1)
//                            .put("_id", 0)), as -> {
//                        if (as.failed()) {
//                            logger.error(as.cause().getMessage(), as);
//                            message.reply(null);
//                        } else {
//                            message.reply(new JsonArray(
//                                    as.result().stream().distinct().collect(Collectors.toList())));
//                        }
//                    });
            message.reply(new JsonArray(ids));
        }
    }


    /**
     * @Description 根据app获取相应设备
     * @author zhang bo
     * @date 18-7-4
     * @version 1.0
     */
    public void getDeviceByApp(List<JsonObject> list, Message<JsonObject> message) {
        List<String> _ids = list.stream().map(e -> new JsonObject(e.toString()).getString("mac"))
                .filter(x -> x != null).collect(Collectors.toList());
        MongoClient.client.findWithOptions(KdsNormalLock.COLLECT_NAME, new JsonObject().put(KdsNormalLock.IS_ADMIN, "1").put(KdsNormalLock.MAC_LOCK
                , new JsonObject().put("$in", new JsonArray(_ids))), new FindOptions().setFields(new JsonObject().put(KdsNormalLock.LOCK_NAME, 1)
                .put(KdsNormalLock.UID, 1).put(KdsNormalLock._ID, 0).put(KdsNormalLock.MAC_LOCK, 1)), rs -> {
            if (rs.failed()) {
                logger.error(rs.cause().getMessage(), rs);
                message.reply(null);
            } else {
                message.reply(new JsonArray(rs.result()));
            }
        });
    }

    /**
     * @Description 記錄審批時間日志
     * @author zhang bo
     * @date 18-4-27
     * @version 1.0
     */
    public void otaApprovateRecord(Message<JsonObject> message) {
        logger.info("params -> {}", message.body());
        MongoClient.client.insert(KdsOTARecord.COLLECT_NAME, message.body(), rs -> {
            if (rs.failed()) logger.error(rs.cause().getMessage(), rs);
        });
    }
}
