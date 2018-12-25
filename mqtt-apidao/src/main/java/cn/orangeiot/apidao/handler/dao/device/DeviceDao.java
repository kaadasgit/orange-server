package cn.orangeiot.apidao.handler.dao.device;

import cn.orangeiot.apidao.client.MongoClient;
import cn.orangeiot.common.genera.ErrorType;
import cn.orangeiot.common.model.SNEntityModel;
import cn.orangeiot.common.utils.KdsCreateRandom;
import cn.orangeiot.common.utils.SHA1;
import cn.orangeiot.common.utils.SNProductUtils;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import scala.Int;
import scala.util.parsing.json.JSONArray;
import scala.util.parsing.json.JSONObject;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static io.vertx.core.Future.succeededFuture;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-01-03
 */
public class DeviceDao {

    private static Logger logger = LogManager.getLogger(DeviceDao.class);

    private Vertx vertx;

    public DeviceDao(Vertx vertx) {
        this.vertx = vertx;
    }

    /**
     * @Description 设备SN号生产
     * @author zhang bo
     * @date 18-1-3
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    @Deprecated
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
                logger.error(ars.cause().getMessage(), ars);
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
                            logger.error(urs.cause().getMessage(), urs);
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
     * @Description 生成測試用戶
     * @author zhang bo
     * @date 18-12-25
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void productionTestUser(Message<JsonObject> message) {
        int count = 0;
        try {
            count = Integer.parseInt(message.body().getString("count"));
        } catch (Exception e) {
            logger.error(e.getCause());
        }

        if (count > 100 || count == 0) {
            message.reply(null);
            return;
        }
        int finalCount = count;
        vertx.executeBlocking(future -> {
            List<BulkOperation> bulkOperations = new ArrayList<>(finalCount);
            JsonArray jsonArray = new JsonArray();
            String usernameBody = message.body().getString("prefix")+KdsCreateRandom.createRandom(5);
            String usernameFuffix = "@testKDS.com";
            String password = message.body().getString("prefix") + "123456";
            String password0 = SHA1.encode(password);
            for (int i = 0; i < finalCount; i++) {
                jsonArray.add(new JsonObject().put("userMail", usernameBody + i + usernameFuffix).put("userPwd", password));
                JsonObject params = new JsonObject().put("type", BulkOperation.BulkOperationType.INSERT).put("document",
                        new JsonObject().put("userMail", usernameBody + i + usernameFuffix).put("userPwd", password0)
                                .put("versionType", "kaadas").put("nickName", usernameBody + i + usernameFuffix)
                        .put("testUser",1)//測試用戶
                ).put("upsert", false).put("multi", false);
                bulkOperations.add(new BulkOperation(params));
            }

            if (bulkOperations.size() > 0)
                MongoClient.client.bulkWrite("kdsUser", bulkOperations, urs -> {//导入账户列表
                    if (urs.failed()) {
                        logger.error(urs.cause().getMessage(), urs);
                        future.complete(null);
                    } else {
                        if (urs.result().getInsertedCount() != 0) {
                            future.complete(jsonArray);
                        } else {
                            future.complete(null);
                        }
                    }
                });

        }, true, (AsyncResult<JsonArray> rs) -> {
            if (rs.failed()) {
                message.reply(null);
            } else {
                if (rs.result() != null) {
                    message.reply(rs.result());
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
    @SuppressWarnings("Duplicates")
    public void productionModelSN(Message<JsonObject> message) {
        vertx.executeBlocking(future -> {
            MongoClient.client.findWithOptions("kdsProductInfoList", new JsonObject().put("modelCode",
                    message.body().getString("model")).put("childCode", message.body().getString("child")),
                    new FindOptions().setFields(new JsonObject().put("time", 1).put("yearCode", 1)
                            .put("weekCode", 1).put("count", 1).put("batch", 1).put("childCode", 1)
                            .put("modelCode", 1)).setSort(new JsonObject().put("time", -1)).setLimit(1), rs -> {
                        if (rs.failed()) {
                            logger.error(rs.cause().getMessage(), rs);
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

                            // 插入数据
                            List<BulkOperation> bulkOperations = new ArrayList<>();
                            List<BulkOperation> GWbulkOperations = new ArrayList<>();
                            String insert_time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                            Map<String, Object> finalMap = map;
                            JsonArray jsonArray = new JsonArray();//返回数据
                            ((List<String>) map.get("snList")).stream().forEach(e -> {
                                String password1 = KdsCreateRandom.randomHexString(24);
                                JsonObject params = new JsonObject().put("type", BulkOperation.BulkOperationType.INSERT).put("document",
                                        new JsonObject().put("SN", e).put("count", finalMap.get("rsCount")).put("yearCode", time
                                        ).put("weekCode", week).put("modelCode", message.body().getString("model"))
                                                .put("childCode", message.body().getString("child")).put("batch", finalMap.get("batchs"))
                                                .put("time", insert_time)
                                                .put("position", SNProductUtils.getPosition(e))).put("upsert", false).put("multi", false);


                                if (message.body().getBoolean("secret")) {//模塊
                                    params.getJsonObject("document").put("password1", password1);
                                    jsonArray.add(new JsonObject().put("SN", e).put("password1", password1));
                                } else {//網關
                                    JsonObject Users = new JsonObject().put("type", BulkOperation.BulkOperationType.INSERT)
                                            .put("document", new JsonObject().put("userGwAccount", e)
                                                    .put("userPwd", SHA1.encode(e)))
                                            .put("upsert", false).put("multi", false);//網關連接賬戶
                                    GWbulkOperations.add(new BulkOperation(Users));
                                    jsonArray.add(new JsonObject().put("SN", e));
                                }
                                bulkOperations.add(new BulkOperation(params));
                            });

                            //網關連接賬戶導入
                            if (GWbulkOperations.size() > 0) {
                                MongoClient.client.bulkWrite("kdsUser", GWbulkOperations, urs -> {//导入账户列表
                                    if (urs.failed()) {
                                        logger.error(urs.cause().getMessage(), urs);
                                        message.reply(null);
                                    } else {
                                        if (urs.result().getInsertedCount() != 0) {
                                            message.reply(new JsonObject());
                                        } else {
                                            message.reply(null);
                                        }
                                    }
                                });
                            }

                            //產品生产信息
                            MongoClient.client.bulkWrite("kdsProductInfoList", bulkOperations, ars -> {
                                if (ars.failed()) logger.error(ars.cause().getMessage(), ars);
                            });

                            //插入產品信息
                            MongoClient.client.insert("kdsModelInfo",
                                    new JsonObject().put("yearCode", time)
                                            .put("weekCode", week).put("modelCode", message.body().getString("model"))
                                            .put("childCode", message.body().getString("child"))
                                            .put("count", message.body().getInteger("count"))
                                            .put("time", insert_time), ars -> {
                                        if (ars.failed()) logger.error(ars.cause().getMessage(), ars);
                                    });//產品相關
                            message.reply(jsonArray);
                        }
                    });
        }, true, null);
    }


    /**
     * @Description 上傳預綁定數據
     * @author zhang bo
     * @date 18-12-21
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public Future<JsonObject> uploadPreBindDevice(List<BulkOperation> bulkOperationList) {
        return Future.future(rs -> {
            MongoClient.client.bulkWriteWithOptions("kdsGatewayDeviceList", bulkOperationList
                    , new BulkWriteOptions().setOrdered(false).setWriteOption(WriteOption.ACKNOWLEDGED), ars -> {
                        if (ars.failed()) {
                            logger.error(ars.cause().getMessage(), ars);
                            rs.fail(ars.cause());
                        } else {
                            JsonObject resultJson = JsonObject.mapFrom(ars.result());
                            logger.debug("=======preBind==mongoBulk============" + resultJson.toString());
                            rs.complete(resultJson);
                        }
                    });
        });
    }


    /**
     * @Description 預先綁定設備
     * @author zhang bo
     * @date 18-12-21
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void preBindDevice(Message<JsonArray> message) {
        vertx.executeBlocking(objectFuture -> {
            final int BOUNART_NUM = 1000;//阀值
            int num = message.body().size();
            int batch = num % BOUNART_NUM == 0 ? num / BOUNART_NUM : num / BOUNART_NUM + 1;//次数

            final List<BulkOperation>[] bulkOperationList = new LinkedList[batch];

            for (int i = 0; i < batch; i++) {
                bulkOperationList[i] = new LinkedList<>();
            }
            AtomicInteger atomicInteger = new AtomicInteger(0);

            final Future<JsonObject>[] future = new Future[batch];//分批回調


            for (int i = 0; i < bulkOperationList.length; i++) {//上傳
                int times;
                if (num < BOUNART_NUM) {
                    times = num;
                } else if (bulkOperationList.length - 1 == i) {
                    if (message.body().size() % BOUNART_NUM == 0)
                        times = BOUNART_NUM;
                    else
                        times = BOUNART_NUM - (BOUNART_NUM * (i + 1) - message.body().size());
                } else {
                    times = BOUNART_NUM;
                }
                for (int j = 0; j < times; j++) {
                    JsonObject datas = message.body().getJsonObject(i * BOUNART_NUM + j);
                    JsonObject params = new JsonObject().put("type", BulkOperation.BulkOperationType.UPDATE)
                            .put("filter", new JsonObject().put("deviceSN", datas.getString("gatewaySN"))
                                    .put("deviceNickName", datas.getString("gatewaySN"))
                                    .put("deviceList", new JsonObject().put("$exists", true)))
                            .put("document", new JsonObject()
                                    .put("$addToSet", new JsonObject().put("deviceList", new JsonObject().put("$each", new JsonArray().add(new JsonObject().put("deviceId", datas.getString("deviceSN"))
                                            .put("event_str", datas.getString("status")).put("macaddr", datas.getString("mac"))
                                            .put("device_type", datas.getString("deviceType")).put("SW", datas.getString("version"))
                                            .put("time", datas.getString("productTime")))))))
                            .put("upsert", true).put("multi", false);
                    bulkOperationList[i].add(new BulkOperation(params));
                }
                future[i] = uploadPreBindDevice(bulkOperationList[i]);
            }

            //处理返回结果
            List<JsonObject> list = new Vector();
            for (int i = 0; i < future.length; i++) {
                future[i].setHandler((AsyncResult<JsonObject> rs) -> {
                    if (rs.failed()) {
                        logger.error(rs.cause().getMessage(), rs);
                        objectFuture.fail(rs.cause());
                    } else {
                        list.add(rs.result());
                        if (atomicInteger.get() == batch - 1) {//最后一次
                            objectFuture.complete(list);
                        }
                        atomicInteger.incrementAndGet();
                    }
                });
            }

        }, true, (AsyncResult<List<JsonObject>> rs) -> {
            if (rs.result() != null && rs.result().size() > 0) {
                int insertTotal = rs.result().stream().mapToInt(count -> count.getJsonArray("upserts").size()).sum();
                int updateTotal = rs.result().stream().mapToInt(count -> count.getInteger("matchedCount")).sum();
                int matchTotal = rs.result().stream().mapToInt(count -> count.getInteger("matchedCount")).sum();
                message.reply(new JsonObject().put("uploadTotal", message.body().size())
                        .put("updateTotal", updateTotal).put("matchTotal", matchTotal).put("insertTotal", insertTotal));
            } else {
                message.reply(new JsonObject().put("uploadTotal", message.body().size())
                        .put("updateTotal", 0).put("matchTotal", 0).put("insertTotal", 0));
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
                        logger.error(rs.cause().getMessage(), rs);
                        message.reply(null);
                    } else {
                        if (Objects.nonNull(rs.result())) {
                            MongoClient.client.updateCollectionWithOptions("kdsProductInfoList"
                                    , new JsonObject().put("SN", message.body().getString("SN"))
                                            .put("password1", message.body().getString("password1")), new JsonObject().put("$set",
                                            new JsonObject().put("mac", message.body().getString("mac"))), new UpdateOptions().setUpsert(true), as -> {
                                        if (as.failed()) {
                                            logger.error(as.cause().getMessage(), as);
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
     * @Description 模块mac地址多写入
     * @author zhang bo
     * @date 18-1-26
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void modelManyMacIn(Message<JsonArray> message) {
        vertx.executeBlocking(res -> {
                    final int BOUNART_NUM = 1000;//阀值
                    int num = message.body().size();
                    int batch = num % BOUNART_NUM == 0 ? num / BOUNART_NUM : num / BOUNART_NUM + 1;//次数

                    final List<BulkOperation>[] bulkOperationList = new LinkedList[batch];

                    for (int i = 0; i < batch; i++) {
                        bulkOperationList[i] = new LinkedList<>();
                    }
                    AtomicInteger atomicInteger = new AtomicInteger(0);

                    final Future<JsonObject>[] future = new Future[batch];//分批回調


                    for (int i = 0; i < bulkOperationList.length; i++) {//上傳
                        int times;
                        if (num < BOUNART_NUM) {
                            times = num;
                        } else if (bulkOperationList.length - 1 == i) {
                            if (message.body().size() % BOUNART_NUM == 0)
                                times = BOUNART_NUM;
                            else
                                times = BOUNART_NUM - (BOUNART_NUM * (i + 1) - message.body().size());
                        } else {
                            times = BOUNART_NUM;
                        }
                        for (int j = 0; j < times; j++) {
                            JsonObject datas = message.body().getJsonObject(i * BOUNART_NUM + j);
                            JsonObject params = new JsonObject().put("type", BulkOperation.BulkOperationType.UPDATE)
                                    .put("filter", new JsonObject().put("SN", datas.getString("SN")).put("password1", datas.getString("Password1")))
                                    .put("document", new JsonObject().put("$set", new JsonObject().put("mac", datas.getString("MAC"))))
                                    .put("upsert", false).put("multi", false);
                            bulkOperationList[i].add(new BulkOperation(params));
                        }
                        future[i] = uploadMac(bulkOperationList[i]);
                    }


                    //处理返回结果
                    List<JsonObject> list = new Vector();
                    for (int i = 0; i < future.length; i++) {
                        future[i].setHandler((AsyncResult<JsonObject> rs) -> {
                            if (rs.failed()) {
                                logger.error(rs.cause().getMessage(), rs);
                                res.fail(rs.cause());
                            } else {
                                list.add(rs.result());
                                if (atomicInteger.get() == batch - 1) {//最后一次
                                    res.complete(list);
                                }
                                atomicInteger.incrementAndGet();
                            }
                        });
                    }
                }, (AsyncResult<List<JsonObject>> rs) -> {
                    if (rs.result().size() > 0) {
                        int insertTotal = rs.result().stream().mapToInt(count -> count.getJsonArray("upserts").size()).sum();
                        int updateTotal = rs.result().stream().mapToInt(count -> count.getInteger("matchedCount")).sum();
                        int matchTotal = rs.result().stream().mapToInt(count -> count.getInteger("matchedCount")).sum();
                        message.reply(new JsonObject().put("uploadTotal", message.body().size())
                                .put("updateTotal", updateTotal).put("matchTotal", matchTotal).put("insertTotal", insertTotal));
                    } else {
                        message.reply(new JsonObject().put("uploadTotal", message.body().size())
                                .put("updateTotal", 0).put("matchTotal", 0).put("insertTotal", 0));
                    }
                }
        );
    }


    /**
     * @Description 上传mac
     * @author zhang bo
     * @date 18-4-26
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public Future<JsonObject> uploadMac(List<BulkOperation> bulkOperationList) {
        return Future.future(rs -> {
            MongoClient.client.bulkWriteWithOptions("kdsProductInfoList", bulkOperationList
                    , new BulkWriteOptions().setOrdered(false).setWriteOption(WriteOption.ACKNOWLEDGED), ars -> {
                        if (ars.failed()) {
                            logger.error(ars.cause().getMessage(), ars);
                            rs.fail(ars.cause());
                        } else {
                            JsonObject resultJson = JsonObject.mapFrom(ars.result());
                            logger.debug("=========mongoBulk============" + resultJson.toString());
                            rs.complete(resultJson);
                        }
                    });
        });
    }


    /**
     * @Description 设备的测试信息写入
     * @author zhang bo
     * @date 18-9-25
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void deviceTestInfoIn(Message<JsonArray> message) {
        vertx.executeBlocking(res -> {
                    final int BOUNART_NUM = 1000;//阀值
                    int num = message.body().size();
                    int batch = num % BOUNART_NUM == 0 ? num / BOUNART_NUM : num / BOUNART_NUM + 1;//次数

                    final List<BulkOperation>[] bulkOperationList = new LinkedList[batch];

                    for (int i = 0; i < batch; i++) {
                        bulkOperationList[i] = new LinkedList<>();
                    }
                    AtomicInteger atomicInteger = new AtomicInteger(0);

                    final Future<JsonObject>[] future = new Future[batch];//分批回調


                    for (int i = 0; i < bulkOperationList.length; i++) {//上傳
                        int times;
                        if (num < BOUNART_NUM) {
                            times = num;
                        } else if (bulkOperationList.length - 1 == i) {
                            if (message.body().size() % BOUNART_NUM == 0)
                                times = BOUNART_NUM;
                            else
                                times = BOUNART_NUM - (BOUNART_NUM * (i + 1) - message.body().size());
                        } else {
                            times = BOUNART_NUM;
                        }
                        for (int j = 0; j < times; j++) {
                            JsonObject datas = message.body().getJsonObject(i * BOUNART_NUM + j);
                            JsonObject params = new JsonObject().put("type", BulkOperation.BulkOperationType.UPDATE)
                                    .put("filter", new JsonObject().put("SN", datas.getString("SN")))
                                    .put("document", new JsonObject().put("$set", datas))
                                    .put("upsert", true).put("multi", false);
                            bulkOperationList[i].add(new BulkOperation(params));
                        }
                        future[i] = uploadDevTestInfo(bulkOperationList[i]);
                    }

                    //处理返回结果
                    List<JsonObject> list = new Vector();
                    for (int i = 0; i < future.length; i++) {
                        future[i].setHandler((AsyncResult<JsonObject> rs) -> {
                            if (rs.failed()) {
                                logger.error(rs.cause().getMessage(), rs);
                                res.fail(rs.cause());
                            } else {
                                list.add(rs.result());
                                if (atomicInteger.get() == batch - 1) {//最后一次
                                    res.complete(list);
                                }
                                atomicInteger.incrementAndGet();
                            }
                        });
                    }
                }, (AsyncResult<List<JsonObject>> rs) -> {
                    if (rs.result().size() > 0) {
                        int insertTotal = rs.result().stream().mapToInt(count -> count.getJsonArray("upserts").size()).sum();
                        int updateTotal = rs.result().stream().mapToInt(count -> count.getInteger("matchedCount")).sum();
                        int matchTotal = rs.result().stream().mapToInt(count -> count.getInteger("matchedCount")).sum();
                        message.reply(new JsonObject().put("uploadTotal", message.body().size())
                                .put("updateTotal", updateTotal).put("matchTotal", matchTotal).put("insertTotal", insertTotal));
                    } else {
                        message.reply(new JsonObject().put("uploadTotal", message.body().size())
                                .put("updateTotal", 0).put("matchTotal", 0).put("insertTotal", 0));
                    }
                }
        );
    }


    /**
     * @Description 上传设备测试信息数据
     * @author zhang bo
     * @date 18-9-25
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public Future<JsonObject> uploadDevTestInfo(List<BulkOperation> bulkOperationList) {
        return Future.future(rs -> {
            MongoClient.client.bulkWriteWithOptions("kdsProductInfoTestList", bulkOperationList
                    , new BulkWriteOptions().setOrdered(false).setWriteOption(WriteOption.ACKNOWLEDGED), ars -> {
                        if (ars.failed()) {
                            logger.error(ars.cause().getMessage(), ars);
                            rs.fail(ars.cause());
                        } else {
                            JsonObject resultJson = JsonObject.mapFrom(ars.result());
                            if (Objects.nonNull(resultJson.getValue("upserts")) && resultJson.getJsonArray("upserts").size() > 0)
                                logger.debug("=========mongoBulk============ upserts -> " + resultJson.getJsonArray("upserts").size());
                            rs.complete(resultJson);
                        }
                    });
        });
    }


    /**
     * @Description 获取mac写入结果
     * @author zhang bo
     * @date 18-1-26
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void getWriteMacResult(Message<JsonObject> message) {
        MongoClient.client.findWithOptions("kdsProductInfoList", new JsonObject()
                        .put("modelCode", message.body().getString("modelCode")).put("childCode", message.body().getString("childCode"))
                        .put("yearCode", message.body().getString("yearCode")).put("weekCode", message.body().getString("weekCode"))
                        .put("mac", new JsonObject().put("$exists", false))
                , new FindOptions().setFields(new JsonObject().put("_id", 0).put("SN", 1)), rs -> {
                    if (rs.failed()) {
                        logger.error(rs.cause().getMessage(), rs);
                    } else {
                        if (rs.result().size() > 0) {
                            message.reply(new JsonArray(rs.result()));
                        } else {
                            message.reply(null);
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
                        logger.error(rs.cause().getMessage(), rs);
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
