package cn.orangeiot.apidao.handler.dao.device;

import cn.orangeiot.apidao.client.MongoClient;
import cn.orangeiot.common.genera.ErrorType;
import cn.orangeiot.common.model.SNEntityModel;
import cn.orangeiot.common.utils.KdsCreateRandom;
import cn.orangeiot.common.utils.SHA1;
import cn.orangeiot.common.utils.SNProductUtils;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.BulkOperation;
import io.vertx.ext.mongo.FindOptions;
import io.vertx.ext.mongo.UpdateOptions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.util.*;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-01-03
 */
public class DeviceDao {

    private static Logger logger = LogManager.getLogger(DeviceDao.class);


    /**
     * @Description 设备SN号生产
     * @author zhang bo
     * @date 18-1-3
     * @version 1.0
     */
    public void productionDeviceSN(Message<JsonObject> message) {
        JsonObject jsonObject = message.body();
        List<BulkOperation> bulkOperations = new ArrayList<>();
        //bulk 批量
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        jsonObject.getJsonArray("deviceSNList").forEach(e -> {
            JsonObject params = new JsonObject().put("type", BulkOperation.BulkOperationType.INSERT)
                    .put("document", new JsonObject().put("deviceSN", e.toString())
                            .put("startTime", time)).put("upsert", false).put("multi", false);
            bulkOperations.add(new BulkOperation(params));
        });

        MongoClient.client.bulkWrite("kdsGatewayList", bulkOperations, ars -> {//导入gatewaysn
            if (ars.failed()) {
                ars.cause().printStackTrace();
                message.reply(null);
            } else {
                if (ars.result().getInsertedCount() != 0) {//生产成功
                    //bulk 批量 导入mqtt连接登录账户
                    jsonObject.getJsonArray("deviceSNList").forEach(e -> {
                        JsonObject params = new JsonObject().put("type", BulkOperation.BulkOperationType.INSERT)
                                .put("document", new JsonObject().put("userGwAccount", e.toString())
                                        .put("userPwd", SHA1.encode(e.toString())))
                                .put("upsert", false).put("multi", false);
                        bulkOperations.add(new BulkOperation(params));
                    });
                    MongoClient.client.bulkWrite("kdsUser", bulkOperations, urs -> {//导入账户列表
                        if (urs.failed()) {
                            urs.cause().printStackTrace();
                            message.reply(null);
                        } else {
                            if (urs.result().getInsertedCount() != 0) {
                                message.reply(new JsonObject());
                            } else {
                                message.reply(null);
                            }
                        }
                    });
                } else {
                    message.reply(null);
                }
            }
        });
    }


    /**
     * @Description 模块生产
     * @author zhang bo
     * @date 18-1-24
     * @version 1.0
     */
    public void productionModelSN(Message<JsonObject> message) {
        MongoClient.client.findWithOptions("kdsProductInfoList", new JsonObject().put("modelCode",
                message.body().getString("model")).put("childCode", message.body().getString("child")),
                new FindOptions().setFields(new JsonObject().put("time", 1).put("yearCode", 1)
                        .put("weekCode", 1).put("count", 1).put("batch", 1).put("childCode", 1)
                        .put("modelCode", 1)).setSort(new JsonObject().put("time", -1)).setLimit(1), rs -> {
                    if (rs.failed()) {
                        rs.cause().printStackTrace();
                        message.reply(null);
                    } else {
                        int dayWeek = LocalDate.now().get(ChronoField.ALIGNED_WEEK_OF_YEAR);//获取周代码
                        String week = String.valueOf(dayWeek).length() == 1 ? "0" + String.valueOf(dayWeek) : String.valueOf(dayWeek);
                        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yy"));//获取年代码

                        Map<String, Object> map;
                        if (rs.result().size() > 0 && Objects.nonNull(rs.result().get(0).getValue("yearCode"))
                                && Objects.nonNull(rs.result().get(0).getValue("weekCode"))) {//存在生产

                            if (rs.result().get(0).getString("yearCode").equals(time) &&
                                    rs.result().get(0).getString("weekCode").equals(week)) //已经存在批次
                                map = SNProductUtils.snDeviceInSN(new SNEntityModel().setProductNum(message.body().getInteger("count"))
                                        .setBatch(rs.result().get(0).getInteger("batch"))
                                        .setStartCount(rs.result().get(0).getInteger("count"))
                                        .setModel(message.body().getString("model")).setChildCode(message.body().getString("child"))
                                        .setWeekCode(week).setYearCode(time));
                            else
                                map = SNProductUtils.snDeviceInSN(new SNEntityModel().setProductNum(message.body().getInteger("count"))
                                        .setBatch(1)
                                        .setStartCount(0)
                                        .setModel(message.body().getString("model")).setChildCode(message.body().getString("child"))
                                        .setWeekCode(week).setYearCode(time));
                        } else {
                            map = SNProductUtils.snDeviceInSN(new SNEntityModel().setProductNum(message.body().getInteger("count"))
                                    .setBatch(1)
                                    .setStartCount(0)
                                    .setModel(message.body().getString("model")).setChildCode(message.body().getString("child"))
                                    .setWeekCode(week).setYearCode(time));
                        }

                        //TODO 插入数据
                        List<BulkOperation> bulkOperations = new ArrayList<>();
                        String insert_time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                        Map<String, Object> finalMap = map;
                        JsonArray jsonArray = new JsonArray();//返回数据
                        ((List<String>) map.get("snList")).stream().forEach(e -> {
                            String password1 = KdsCreateRandom.randomHexString(24);
                            JsonObject params = new JsonObject().put("type", BulkOperation.BulkOperationType.INSERT).put("document",
                                    new JsonObject().put("SN", e).put("count", finalMap.get("rsCount")).put("yearCode", time
                                    ).put("weekCode", week).put("modelCode", message.body().getString("model"))
                                            .put("childCode", message.body().getString("child")).put("batch", finalMap.get("batchs"))
                                            .put("time", insert_time)).put("upsert", false).put("multi", false);
                            if (message.body().getBoolean("secret")) {
                                params.getJsonObject("document").put("password1", password1);
                                jsonArray.add(new JsonObject().put("SN", e).put("password1", password1));
                            } else {
                                jsonArray.add(new JsonObject().put("SN", e));
                            }
                            bulkOperations.add(new BulkOperation(params));

                        });
                        MongoClient.client.bulkWrite("kdsProductInfoList", bulkOperations, ars -> {
                            if (ars.failed()) ars.cause().printStackTrace();
                        });

                        //插入產品信息
                        MongoClient.client.insert("kdsModelInfo",
                                new JsonObject().put("yearCode", time)
                                        .put("weekCode", week).put("modelCode", message.body().getString("model"))
                                        .put("childCode", message.body().getString("child"))
                                        .put("count", message.body().getInteger("count"))
                                        .put("time", insert_time), ars -> {
                                    if (ars.failed()) ars.cause().printStackTrace();
                                });//產品相關
                        message.reply(jsonArray);
                    }
                });
    }


    /**
     * @Description 模块mac地址写入
     * @author zhang bo
     * @date 18-1-26
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void modelMacIn(Message<JsonObject> message) {
        MongoClient.client.findOne("kdsProductInfoList", new JsonObject().put("SN",
                message.body().getString("SN")).put("password1", message.body().getString("password1")),
                new JsonObject().put("_id", 1), rs -> {
                    if (rs.failed()) {
                        rs.cause().printStackTrace();
                        message.reply(null);
                    } else {
                        if (Objects.nonNull(rs.result())) {
                            MongoClient.client.updateCollectionWithOptions("kdsProductInfoList"
                                    , new JsonObject().put("SN", message.body().getString("SN"))
                                            .put("password1", message.body().getString("password1")), new JsonObject().put("$set",
                                            new JsonObject().put("mac", message.body().getString("mac"))), new UpdateOptions().setUpsert(true), as -> {
                                        if (as.failed()) {
                                            as.cause().printStackTrace();
                                            message.reply(null);
                                        } else {
                                            message.reply(new JsonObject());
                                        }
                                    });
                        } else {
                            message.reply(null, new DeliveryOptions().addHeader("code", String.valueOf(ErrorType.DATA_MAP_FAIL.getKey()))
                                    .addHeader("msg", ErrorType.DATA_MAP_FAIL.getValue()));
                        }
                    }
                });
    }


    /**
     * @Description 根据SN获取模块的password1
     * @author zhang bo
     * @date 18-1-26
     * @version 1.0
     */
    public void getPwdByMac(Message<JsonObject> message) {
        MongoClient.client.findOne("kdsProductInfoList", new JsonObject().put("SN",
                message.body().getString("SN")),
                new JsonObject().put("_id", 0).put("password1", 1), rs -> {
                    if (rs.failed()) {
                        rs.cause().printStackTrace();
                        message.reply(null);
                    } else {
                        if (Objects.nonNull(rs.result())) {
                            message.reply(rs.result());
                        } else {
                            message.reply(null, new DeliveryOptions().addHeader("code", String.valueOf(ErrorType.SELECT_DATA_NULL.getKey()))
                                    .addHeader("msg", ErrorType.SELECT_DATA_NULL.getValue()));
                        }
                    }
                });

    }
}
