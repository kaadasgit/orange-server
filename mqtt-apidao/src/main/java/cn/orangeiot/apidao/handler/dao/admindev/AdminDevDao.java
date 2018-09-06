package cn.orangeiot.apidao.handler.dao.admindev;

import cn.orangeiot.apidao.client.MongoClient;
import cn.orangeiot.apidao.client.RedisClient;
import cn.orangeiot.apidao.conf.RedisKeyConf;
import cn.orangeiot.common.genera.ErrorType;
import cn.orangeiot.reg.adminlock.AdminlockAddr;
import cn.orangeiot.reg.message.MessageAddr;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.BulkOperation;
import io.vertx.ext.mongo.FindOptions;
import io.vertx.ext.mongo.UpdateOptions;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import scala.util.parsing.json.JSONObject;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2017-12-22
 */
public class AdminDevDao implements AdminlockAddr, MessageAddr {
    private static Logger logger = LogManager.getLogger(AdminDevDao.class);

    private Vertx vertx;

    public AdminDevDao(Vertx vertx) {
        this.vertx = vertx;
    }

    /**
     * @Description 管理员设备添加
     * @author zhang bo
     * @date 17-12-22
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void createAdminDev(Message<JsonObject> message) {
        JsonObject jsonObject = message.body();
        //设备注册
        MongoClient.client.insert("kdsDeviceList", new JsonObject().put("lockName", jsonObject.getString("devname"))
                .put("uname", jsonObject.getString("user_id")).put("model"
                        , Objects.nonNull(jsonObject.getValue("model")) ? jsonObject.getString("model") : ""), ars -> {
            if (ars.failed()) {
                logger.error(ars.cause().getMessage(), ars);
                message.reply(null);
            } else {
                String macLock = "";
                if (jsonObject.getString("devmac") != null)
                    macLock = jsonObject.getString("devmac");
                else
                    macLock = jsonObject.getString("devname");
                String finalMacLock = macLock;
                RedisClient.client.hget(RedisKeyConf.USER_ACCOUNT + jsonObject.getString("user_id"), RedisKeyConf.USER_VAL_INFO, rrs -> {
                    if (rrs.failed()) {
                        logger.error(rrs.cause().getMessage(), rrs);
                    } else {
                        JsonObject userInfo = new JsonObject(rrs.result());
                        String adminname = userInfo.getString("username");

                        MongoClient.client.findOne("kdsNormalLock", new JsonObject().put("lockName", jsonObject.getString("devname"))
                                , new JsonObject().put("_id", 1), flag -> {
                                    if (flag.failed()) {
                                        logger.error(flag.cause().getMessage(), flag);
                                    } else {
                                        if (!Objects.nonNull(flag.result())) {
                                            MongoClient.client.insert("kdsNormalLock", new JsonObject().put("lockName"
                                                    , jsonObject.getString("devname")).put("lockNickName", jsonObject.getString("devname"))
                                                            .put("versionType", message.body().getString("versionType"))
                                                            .put("macLock", finalMacLock).put("adminuid", jsonObject.getString("user_id"))
                                                            .put("adminname", adminname).put("adminnickname", userInfo.getString("nickName"))
                                                            .put("uid", jsonObject.getString("user_id")).put("uname", adminname).put("unickname", userInfo.getString("nickName"))
                                                            .put("open_purview", "3").put("is_admin", "1").put("datestart", "0").put("dateend", "0").put("center_latitude", "0")
                                                            .put("center_longitude", "0").put("edge_latitude", "0").put("edge_longitude", "0").put("circle_radius", "0").put("auto_lock", "0")
                                                            .put("items", new JsonArray().add("0").add("0").add("0").add("0").add("0").add("0").add("0"))
                                                            .put("password1", Objects.nonNull(jsonObject.getValue("password1")) ? jsonObject.getString("password1") : "")
                                                            .put("password2", Objects.nonNull(jsonObject.getValue("password2")) ? jsonObject.getString("password2") : "")
                                                            .put("model", Objects.nonNull(jsonObject.getValue("model")) ? jsonObject.getString("model") : "")
                                                    , res -> {
                                                        if (res.failed()) {
                                                            logger.error(res.cause().getMessage(), res);
                                                            message.reply(null);
                                                        } else {
                                                            if (Objects.nonNull(res.result())) {
                                                                message.reply(new JsonObject());
                                                            } else {
                                                                message.reply(null);
                                                            }
                                                        }
                                                    });
                                        } else {
                                            message.reply(null, new DeliveryOptions().addHeader("code",
                                                    String.valueOf(ErrorType.BINDED.getKey())).addHeader("msg", ErrorType.BINDED.getValue()));

                                        }
                                    }
                                });
                    }
                });
            }
        });

    }


    /**
     * @Description 第三方重置设备
     * @author zhang bo
     * @date 17-12-22
     * @version 1.0
     */
    public void deletevendorDev(Message<JsonObject> message) {
        JsonObject jsonObject = message.body();
        MongoClient.client.removeDocuments("kdsNormalLock", new JsonObject().put("lockName", jsonObject.getString("devname"))
                , rs -> {
                    if (rs.failed()) logger.error(rs.cause().getMessage(), rs);
                });
        MongoClient.client.removeDocuments("kdsDeviceList", new JsonObject().put("lockName", jsonObject.getString("devname"))
                , rs -> {
                    if (rs.failed()) logger.error(rs.cause().getMessage(), rs);
                });
        MongoClient.client.removeDocuments("kdsOpenLockList", new JsonObject().put("lockName", jsonObject.getString("devname"))
                , rs -> {
                    if (rs.failed()) logger.error(rs.cause().getMessage(), rs);
                });
        message.reply(new JsonObject());
    }


    /**
     * @Description 用户主动删除设备
     * @author zhang bo
     * @date 17-12-24
     * @version 1.0
     */
    public void deleteAdminDev(Message<JsonObject> message) {
        JsonObject jsonObject = message.body();
        //查找是否是管理员删除
        MongoClient.client.findOne("kdsNormalLock", new JsonObject().put("lockName", jsonObject.getString("devname"))
                        .put("adminuid", new JsonObject().put("$exists", true)),
                new JsonObject().put("adminuid", 1), ars -> {
                    if (ars.failed()) {
                        logger.error(ars.cause().getMessage(), ars);
                        message.reply(null);
                    } else {
                        if (Objects.nonNull(ars.result().getValue("adminuid"))) {
                            message.reply(new JsonObject());
                            if (ars.result().getString("adminuid")
                                    .equals(jsonObject.getString("adminid"))) {//管理员删除设备
                                MongoClient.client.removeDocuments("kdsNormalLock", new JsonObject().put("lockName", jsonObject.getString("devname"))
                                        , rs -> {
                                            if (rs.failed()) logger.error(rs.cause().getMessage(), rs);
                                        });
                                MongoClient.client.removeDocuments("kdsDeviceList", new JsonObject().put("lockName", jsonObject.getString("devname"))
                                        , rs -> {
                                            if (rs.failed()) logger.error(rs.cause().getMessage(), rs);
                                        });
                                MongoClient.client.removeDocuments("kdsOpenLockList", new JsonObject().put("lockName", jsonObject.getString("devname"))
//                                                .put("versionType", message.body().getString("versionType"))
                                        , rs -> {
                                            if (rs.failed()) logger.error(rs.cause().getMessage(), rs);
                                        });
                            } else {//普通用户删除设备
                                MongoClient.client.removeDocument("kdsNormalLock", new JsonObject().put("lockName", jsonObject.getString("devname"))
                                                .put("uid", jsonObject.getString("adminid"))
                                        , rs -> {
                                            if (rs.failed()) logger.error(rs.cause().getMessage(), rs);
                                        });
                            }
                        }
                    }
                });
    }


    /**
     * @Description 管理员删除用户
     * @author zhang bo
     * @date 17-12-24
     * @version 1.0
     */
    public void deleteNormalDev(Message<JsonObject> message) {
        JsonObject jsonObject = message.body();
        MongoClient.client.findOne("kdsUser", new JsonObject().put("versionType", message.body().getString("versionType"))
                        .put("$or", new JsonArray().add(new JsonObject().put("userTel",
                                jsonObject.getString("dev_username"))).add(new JsonObject().put("userMail", jsonObject.getString("dev_username")))),
                new JsonObject().put("_id", 1), ars -> {//获取device_username的用户信息
                    if (ars.failed()) {
                        logger.error(ars.cause().getMessage(), ars);
                    } else {
                        MongoClient.client.removeDocument("kdsNormalLock", new JsonObject().put("lockName", jsonObject.getString("devname"))
                                .put("uid", ars.result().getString("_id")), rs -> {
                            if (rs.failed()) logger.error(rs.cause().getMessage(), rs);
                            else
                                message.reply(new JsonObject());
                        });
                    }
                });
    }


    /**
     * @Description 管理员为设备添加普通用户
     * @author zhang bo
     * @date 17-12-24
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void createNormalDev(Message<JsonObject> message) {
        JsonObject jsonObject = message.body();
        String macLock = "";
        String lockNickName = "";
        if (Objects.nonNull(jsonObject.getString("devicemac")))
            macLock = jsonObject.getString("devicemac");
        else
            macLock = jsonObject.getString("devname");
        if (Objects.nonNull(jsonObject.getValue("lockNickName")))
            lockNickName = jsonObject.getString("lockNickName");
        else
            lockNickName = jsonObject.getString("devname");
        String finalMacLock = macLock;
        String finalLockNickName = lockNickName;
        MongoClient.client.findOne("kdsUser", new JsonObject().put("versionType", message.body().getString("versionType"))
                        .put("$or", new JsonArray().add(new JsonObject().put("userTel"
                                , jsonObject.getString("device_username"))).add(new JsonObject().put("userMail", jsonObject.getString("device_username")))),
                new JsonObject().put("nickName", 1).put("userMail", 1).put("userTel", 1), ars -> {//获取device_username的用户信息
                    if (ars.failed()) {
                        logger.error(ars.cause().getMessage(), ars);
                    } else {
                        if (Objects.nonNull(ars.result())) {
                            MongoClient.client.findOne("kdsNormalLock", new JsonObject().put("adminuid", jsonObject.getString("admin_id")).put("lockName", jsonObject.getString("devname"))
                                    , new JsonObject(), returnData -> {
                                        if (returnData.failed()) {
                                            logger.error(returnData.cause().getMessage(), returnData);
                                        } else {
                                            if (Objects.nonNull(returnData.result())) {//是否是锁的管理员
                                                String adminname = returnData.result().getString("adminname");
                                                String uname = "";
                                                if (Objects.nonNull(ars.result().getValue("userMail")))
                                                    uname = ars.result().getString("userMail");
                                                else
                                                    uname = ars.result().getString("userTel");
                                                JsonObject paramsJsonObject = new JsonObject().put("lockName"
                                                        , jsonObject.getString("devname")).put("lockNickName", finalLockNickName)
                                                        .put("macLock", finalMacLock).put("adminuid", jsonObject.getString("admin_id"))
                                                        .put("adminname", adminname).put("adminnickname", returnData.result().getString("adminnickname"))
                                                        .put("uid", ars.result().getString("_id")).put("uname", uname).put("unickname", ars.result().getString("nickName"))
                                                        .put("open_purview", jsonObject.getString("open_purview")).put("is_admin", "0").put("datestart", jsonObject.getString("start_time"))
                                                        .put("dateend", jsonObject.getString("end_time")).put("auto_lock", "0").put("items", jsonObject.getJsonArray("items"))
                                                        .put("password1", Objects.nonNull(returnData.result().getValue("password1")) ? returnData.result().getString("password1") : "")
                                                        .put("password2", Objects.nonNull(returnData.result().getValue("password2")) ? returnData.result().getString("password2") : "")
                                                        .put("versionType", message.body().getString("versionType"))
                                                        .put("model", Objects.nonNull(returnData.result().getValue("model")) ? returnData.result().getString("model") : "");

                                                // 查询是否已经添加过的账号
                                                MongoClient.client.findOne("kdsNormalLock", new JsonObject().put("lockName", jsonObject.getString("devname"))
                                                        .put("uname", jsonObject.getString("device_username")), new JsonObject().put("_id", 1), drs -> {
                                                    if (drs.failed()) {
                                                        logger.error(drs.cause().getMessage(), drs);
                                                    } else {
                                                        if (!Objects.nonNull(drs.result())) {
                                                            if (jsonObject.getString("open_purview").equals("3")) {//查詢設備信息
                                                                MongoClient.client.findOne("kdsNormalLock", new JsonObject().put("lockName", jsonObject.getString("devname"))
                                                                        .put("uid", jsonObject.getString("admin_id")), new JsonObject().put("center_latitude", 1)
                                                                        .put("center_longitude", 1).put("edge_latitude", 1).put("edge_longitude", 1).put("circle_radius", 1), rs -> {
                                                                    if (rs.failed()) {
                                                                        logger.error(rs.cause().getMessage(), rs);
                                                                    } else {
                                                                        paramsJsonObject.put("center_latitude", rs.result().getString("center_latitude"))
                                                                                .put("center_longitude", rs.result().getString("center_longitude")).put("edge_latitude", rs.result().getString("edge_latitude"))
                                                                                .put("edge_longitude", rs.result().getString("edge_longitude")).put("circle_radius", rs.result().getString("circle_radius"));
                                                                        MongoClient.client.insert("kdsNormalLock", paramsJsonObject, res -> {
                                                                            if (res.failed()) {
                                                                                logger.error(res.cause().getMessage(), res);
                                                                                message.reply(null, new DeliveryOptions().addHeader("code",
                                                                                        String.valueOf(ErrorType.DEV_REQUEST_FAIL.getKey())).addHeader("msg", ErrorType.DEV_REQUEST_FAIL.getValue()));

                                                                            } else {
                                                                                if (Objects.nonNull(res.result())) {
                                                                                    message.reply(new JsonObject());
                                                                                } else {
                                                                                    message.reply(null, new DeliveryOptions().addHeader("code",
                                                                                            String.valueOf(ErrorType.DEV_REQUEST_FAIL.getKey())).addHeader("msg", ErrorType.DEV_REQUEST_FAIL.getValue()));

                                                                                }
                                                                            }
                                                                        });
                                                                    }

                                                                });
                                                            } else {
                                                                paramsJsonObject.put("center_latitude", "0")
                                                                        .put("center_longitude", "0").put("edge_latitude", "0").put("edge_longitude", "0").put("circle_radius", "0");
                                                                MongoClient.client.insert("kdsNormalLock", paramsJsonObject, res -> {
                                                                    if (res.failed()) {
                                                                        logger.error(res.cause().getMessage(), res);
                                                                        message.reply(null, new DeliveryOptions().addHeader("code",
                                                                                String.valueOf(ErrorType.DEV_REQUEST_FAIL.getKey())).addHeader("msg", ErrorType.DEV_REQUEST_FAIL.getValue()));
                                                                    } else {
                                                                        if (Objects.nonNull(res.result())) {
                                                                            message.reply(new JsonObject());
                                                                        } else {
                                                                            message.reply(null, new DeliveryOptions().addHeader("code",
                                                                                    String.valueOf(ErrorType.DEV_REQUEST_FAIL.getKey())).addHeader("msg", ErrorType.DEV_REQUEST_FAIL.getValue()));

                                                                        }
                                                                    }
                                                                });
                                                            }
                                                        } else {
                                                            message.reply(null, new DeliveryOptions().addHeader("code",
                                                                    String.valueOf(ErrorType.USERNAME_INVALID_FAIL.getKey())).addHeader("msg", ErrorType.USERNAME_INVALID_FAIL.getValue()));
                                                        }
                                                    }
                                                });
                                            } else {
                                                message.reply(null, new DeliveryOptions().addHeader("code",
                                                        String.valueOf(ErrorType.NO_ADMIN_FAIL.getKey())).addHeader("msg", ErrorType.NO_ADMIN_FAIL.getValue()));
                                            }
                                        }
                                    });
                        } else {
                            message.reply(null, new DeliveryOptions().addHeader("code",
                                    String.valueOf(ErrorType.EXCEPTION.getKey())).addHeader("msg", ErrorType.EXCEPTION.getValue()));
                        }
                    }
                });
    }


    /**
     * @Description 获取开锁记录
     * @author zhang bo
     * @date 17-12-24
     * @version 1.0
     */
    public void downloadOpenLocklist(Message<JsonObject> message) {
        JsonObject jsonObject = message.body();
        Integer pageNum = Integer.parseInt(jsonObject.getString("pagenum"));
        MongoClient.client.findOne("kdsNormalLock", new JsonObject().put("lockName", jsonObject.getString("device_name"))
                        .put("adminuid", new JsonObject().put("$exists", true)).put("uid", jsonObject.getString("user_id")),
                new JsonObject().put("adminuid", 1).put("_id", 0).put("uname", 1), ars -> {
                    if (ars.failed()) {
                        logger.error(ars.cause().getMessage(), ars);
                        message.reply(null);
                    } else {
                        JsonObject paramsJsonObject = new JsonObject()
                                .put("versionType", message.body().getString("versionType"));
                        // 根据不同权限查询记录
                        if (Objects.nonNull(ars.result()) && ars.result().getString("adminuid").equals(jsonObject.getString("user_id")))
                            paramsJsonObject.put("lockName", jsonObject.getString("device_name"));
                        else
                            paramsJsonObject.put("lockName", jsonObject.getString("device_name")).put("uname", ars.result().getString("uname"));

                        MongoClient.client.findWithOptions("kdsOpenLockList", paramsJsonObject,
                                new FindOptions().setLimit(20).setSkip((pageNum - 1) * 20).setSort(
                                        new JsonObject().put("open_time", -1)), rs -> {
                                    if (rs.failed()) {
                                        logger.error(rs.cause().getMessage(), rs);
                                        message.reply(null);
                                    } else {
                                        message.reply(new JsonArray(rs.result()));
                                    }
                                });
                    }
                });
    }


    /**
     * @Description 管理员修改普通用户权限 open_purview 1表示1次  2表示多次 3表示永久
     * @author zhang bo
     * @date 17-12-24
     * @version 1.0
     */
    public void updateNormalDevlock(Message<JsonObject> message) {
        JsonObject jsonObject = message.body();
        JsonObject paramsJsonObject = new JsonObject().put("$set", new JsonObject().put("datestart", Objects.nonNull(jsonObject.getValue("datestart"))
                ? jsonObject.getString("datestart") : "0")
                .put("dateend", Objects.nonNull(jsonObject.getValue("dateend"))
                        ? jsonObject.getString("dateend") : "0").put("open_purview", jsonObject.getString("open_purview"))
                .put("items", Objects.nonNull(jsonObject.getValue("items")) ? jsonObject.getJsonArray("items")
                        : new JsonArray().add("0").add("0").add("0").add("0").add("0").add("0").add("0")));
        MongoClient.client.updateCollection("kdsNormalLock", new JsonObject().put("uname", jsonObject.getString("dev_username"))
                .put("lockName", jsonObject.getString("devname")), paramsJsonObject, rs -> {
            if (rs.failed()) {
                logger.error(rs.cause().getMessage(), rs);
                message.reply(null);
            } else {
                message.reply(new JsonObject());
            }
        });
    }


    /**
     * @Description 用户申请开锁
     * @author zhang bo
     * @date 17-12-24
     * @version 1.0
     */
    public void adminOpenLock(Message<JsonObject> message) {
        JsonObject jsonObject = message.body();
        if (jsonObject.getString("is_admin").equals("1")) {//管理員
            MongoClient.client.findOne("kdsNormalLock", new JsonObject().put("lockName", jsonObject.getString("devname"))
                            .put("adminuid", new JsonObject().put("$exists", true)),
                    new JsonObject().put("adminname", 1).put("lockNickName", 1).put("adminnickname", 1), ars -> {
                        if (ars.failed()) {
                            logger.error(ars.cause().getMessage(), ars);
                        } else {
                            if (Objects.nonNull(ars.result())) {
                                if (!jsonObject.getString("open_type").equals("100")) {//不上傳開鎖記錄，會重復
                                    MongoClient.client.insert("kdsOpenLockList", new JsonObject().put("lockName", jsonObject.getString("devname"))
                                            .put("versionType", message.body().getString("versionType"))
                                            .put("lockNickName", ars.result().getString("lockNickName")).put("nickName", ars.result().getString("adminnickname"))
                                            .put("uname", ars.result().getString("adminname")).put("open_purview", "3").put("open_time", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()))
                                            .put("open_type", jsonObject.getString("open_type")), rs -> {
                                        if (rs.failed()) {
                                            logger.error(rs.cause().getMessage(), rs);
                                        } else {
                                            message.reply(new JsonObject());
                                        }
                                    });
                                } else {
                                    message.reply(new JsonObject());
                                }
                            } else
                                message.reply(null, new DeliveryOptions()
                                        .addHeader("code", String.valueOf(ErrorType.OPEN_LOCK__NOTFAIL.getKey()))
                                        .addHeader("msg", ErrorType.OPEN_LOCK__NOTFAIL.getValue()));
                        }
                    });
        } else {//普通用戶申請開鎖
            MongoClient.client.findOne("kdsNormalLock", new JsonObject().put("lockName", jsonObject.getString("devname")).put("uid", jsonObject.getString("user_id"))
                    , new JsonObject().put("uname", 1).put("lockNickName", 1).put("unickname", 1).put("open_purview", 1)
                            .put("datestart", 1).put("dateend", 1).put("items", 1), ars -> {
                        if (ars.failed()) {
                            logger.error(ars.cause().getMessage(), ars);
                        } else {
                            JsonObject paramsJsoNObject = new JsonObject().put("lockName", jsonObject.getString("devname"))
                                    .put("versionType", message.body().getString("versionType"))
                                    .put("lockNickName", ars.result().getString("lockNickName")).put("nickName", ars.result().getString("unickname"))
                                    .put("uname", ars.result().getString("uname")).put("open_type", jsonObject.getString("open_type"));

                            switch (ars.result().getString("open_purview")) {
                                case "1":
                                    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");//设置日期格式
                                    long todaytime = new Date().getTime();
                                    try {
                                        if (todaytime > df.parse(ars.result().getString("datestart")).getTime() && todaytime < df.parse(ars.result().getString("dateend")).getTime()) {
                                            paramsJsoNObject.put("open_purview", "1").put("open_time", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
                                            if (!jsonObject.getString("open_type").equals("100")) {//不上傳開鎖記錄，會重復
                                                MongoClient.client.insert("kdsOpenLockList", paramsJsoNObject, rs -> {
                                                    if (rs.failed()) logger.error(rs.cause().getMessage(), rs);
                                                });
                                            }
                                            MongoClient.client.updateCollection("kdsNormalLock", new JsonObject().put("uid", jsonObject.getString("user_id"))
                                                            .put("lockName", jsonObject.getString("devname")), new JsonObject().put("$set", new JsonObject().put("open_purview", "4"))
                                                    , rs -> {
                                                        if (rs.failed()) logger.error(rs.cause().getMessage(), rs);
                                                        else
                                                            message.reply(new JsonObject());
                                                    });
//                                            vertx.eventBus().send(MessageAddr.class.getName() + SEND_APPLICATION_NOTIFY, new JsonObject().put("uid", jsonObject.getString("user_id")));
                                        } else {//開鎖失敗
                                            message.reply(null, new DeliveryOptions()
                                                    .addHeader("code", String.valueOf(ErrorType.OPEN_LOCK_FAIL.getKey()))
                                                    .addHeader("msg", ErrorType.OPEN_LOCK_FAIL.getValue()));
                                        }
                                    } catch (Exception e) {
                                        logger.error(e.getMessage(), e);
                                        message.reply(null, new DeliveryOptions()
                                                .addHeader("code", String.valueOf(ErrorType.OPEN_LOCK_FAIL.getKey()))
                                                .addHeader("msg", ErrorType.OPEN_LOCK_FAIL.getValue()));
                                    }
                                    break;
                                case "2":
                                    //设置日期格式
                                    Calendar cal = Calendar.getInstance();
                                    try {
                                        Date startday = new SimpleDateFormat("HH:mm").parse(ars.result().getString("datestart"));
                                        Date endday = new SimpleDateFormat("HH:mm").parse(ars.result().getString("dateend"));
                                        Date todaytmp = new SimpleDateFormat("HH:mm").parse(Integer.toString(new Date().getHours()) + ":" + Integer.toString(new Date().getMinutes()));
                                        //  1 才能开锁   默认 星期天 是第一位
                                        cal.setTime(new Date());
                                        int week_index = cal.get(Calendar.DAY_OF_WEEK) - 1;
                                        if (ars.result().getJsonArray("items").getList().get(week_index).equals("1")) {
                                            if (todaytmp.getTime() > startday.getTime() && todaytmp.getTime() < endday.getTime()) {
                                                paramsJsoNObject.put("open_purview", "2").put("open_time", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
                                                if (!jsonObject.getString("open_type").equals("100")) {//不上傳開鎖記錄，會重復
                                                    MongoClient.client.insert("kdsOpenLockList", paramsJsoNObject, rs -> {
                                                        if (rs.failed()) logger.error(rs.cause().getMessage(), rs);
                                                    });
                                                }
                                                message.reply(new JsonObject());
//                                                vertx.eventBus().send(MessageAddr.class.getName() + SEND_APPLICATION_NOTIFY, new JsonObject().put("uid", jsonObject.getString("user_id")));
                                            } else {
                                                message.reply(null, new DeliveryOptions()
                                                        .addHeader("code", String.valueOf(ErrorType.OPEN_LOCK_FAIL.getKey()))
                                                        .addHeader("msg", ErrorType.OPEN_LOCK_FAIL.getValue()));
                                            }
                                        } else {
                                            message.reply(null, new DeliveryOptions()
                                                    .addHeader("code", String.valueOf(ErrorType.OPEN_LOCK_FAIL.getKey()))
                                                    .addHeader("msg", ErrorType.OPEN_LOCK_FAIL.getValue()));
                                        }
                                    } catch (Exception e) {
                                        logger.error(e.getMessage(), e);
                                        message.reply(null, new DeliveryOptions()
                                                .addHeader("code", String.valueOf(ErrorType.OPEN_LOCK_FAIL.getKey()))
                                                .addHeader("msg", ErrorType.OPEN_LOCK_FAIL.getValue()));
                                    }
                                    break;
                                case "4":
                                    message.reply(null, new DeliveryOptions()
                                            .addHeader("code", String.valueOf(ErrorType.OPEN_LOCK__NOTFAIL.getKey()))
                                            .addHeader("msg", ErrorType.OPEN_LOCK__NOTFAIL.getValue()));
                                    break;
                                default:
                                    paramsJsoNObject.put("open_purview", "3").put("open_time", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
                                    if (!jsonObject.getString("open_type").equals("100")) {//不上傳開鎖記錄，會重復
                                        MongoClient.client.insert("kdsOpenLockList", paramsJsoNObject, rs -> {
                                            if (rs.failed()) logger.error(rs.cause().getMessage(), rs);
                                        });
                                    }
//                                    vertx.eventBus().send(MessageAddr.class.getName() + SEND_APPLICATION_NOTIFY, new JsonObject().put("uid", jsonObject.getString("user_id")));
                                    message.reply(new JsonObject());
                                    break;
                            }
                        }
                    });
        }
    }


    /**
     * @Description 开锁鉴权
     * @author zhang bo
     * @date 18-5-30
     * @version 1.0
     */
    public void openLockAuth(Message<JsonObject> message) {
        JsonObject jsonObject = message.body();
        if (jsonObject.getString("is_admin").equals("1")) {//管理員
            MongoClient.client.findOne("kdsNormalLock", new JsonObject().put("lockName", jsonObject.getString("devname"))
                            .put("adminuid", new JsonObject().put("$exists", true)),
                    new JsonObject().put("adminname", 1).put("lockNickName", 1).put("adminnickname", 1), ars -> {
                        if (ars.failed()) {
                            logger.error(ars.cause().getMessage(), ars);
                        } else {
                            if (Objects.nonNull(ars.result())) {
                                message.reply(new JsonObject());
                            } else {
                                message.reply(null, new DeliveryOptions()
                                        .addHeader("code", String.valueOf(ErrorType.OPEN_LOCK__NOTFAIL.getKey()))
                                        .addHeader("msg", ErrorType.OPEN_LOCK__NOTFAIL.getValue()));
                            }
                        }
                    });
        } else {//普通用戶申請開鎖
            MongoClient.client.findOne("kdsNormalLock", new JsonObject().put("lockName", jsonObject.getString("devname")).put("uid", jsonObject.getString("user_id"))
                    , new JsonObject().put("uname", 1).put("lockNickName", 1).put("unickname", 1).put("open_purview", 1)
                            .put("datestart", 1).put("dateend", 1).put("items", 1), ars -> {
                        if (ars.failed()) {
                            logger.error(ars.cause().getMessage(), ars);
                        } else {
                            switch (ars.result().getString("open_purview")) {
                                case "1":
                                    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");//设置日期格式
                                    long todaytime = new Date().getTime();
                                    try {
                                        if (todaytime > df.parse(ars.result().getString("datestart")).getTime() && todaytime < df.parse(ars.result().getString("dateend")).getTime()) {
                                            message.reply(new JsonObject());
                                        } else {//鉴权失敗
                                            message.reply(null, new DeliveryOptions()
                                                    .addHeader("code", String.valueOf(ErrorType.OPEN_LOCK_FAIL.getKey()))
                                                    .addHeader("msg", ErrorType.OPEN_LOCK_FAIL.getValue()));
                                        }
                                    } catch (Exception e) {
                                        logger.error(e.getMessage(), e);
                                        message.reply(null, new DeliveryOptions()
                                                .addHeader("code", String.valueOf(ErrorType.OPEN_LOCK_FAIL.getKey()))
                                                .addHeader("msg", ErrorType.OPEN_LOCK_FAIL.getValue()));
                                    }
                                    break;
                                case "2":
                                    //设置日期格式
                                    Calendar cal = Calendar.getInstance();
                                    try {
                                        Date startday = new SimpleDateFormat("HH:mm").parse(ars.result().getString("datestart"));
                                        Date endday = new SimpleDateFormat("HH:mm").parse(ars.result().getString("dateend"));
                                        Date todaytmp = new SimpleDateFormat("HH:mm").parse(Integer.toString(new Date().getHours()) + ":" + Integer.toString(new Date().getMinutes()));
                                        //  1 才能开锁   默认 星期天 是第一位
                                        cal.setTime(new Date());
                                        int week_index = cal.get(Calendar.DAY_OF_WEEK) - 1;
                                        if (ars.result().getJsonArray("items").getList().get(week_index).equals("1")) {
                                            if (todaytmp.getTime() > startday.getTime() && todaytmp.getTime() < endday.getTime()) {
                                                message.reply(new JsonObject());
                                            } else {
                                                message.reply(null, new DeliveryOptions()
                                                        .addHeader("code", String.valueOf(ErrorType.OPEN_LOCK_FAIL.getKey()))
                                                        .addHeader("msg", ErrorType.OPEN_LOCK_FAIL.getValue()));
                                            }
                                        } else {
                                            message.reply(null, new DeliveryOptions()
                                                    .addHeader("code", String.valueOf(ErrorType.OPEN_LOCK_FAIL.getKey()))
                                                    .addHeader("msg", ErrorType.OPEN_LOCK_FAIL.getValue()));
                                        }
                                    } catch (Exception e) {
                                        logger.error(e.getMessage(), e);
                                        message.reply(null, new DeliveryOptions()
                                                .addHeader("code", String.valueOf(ErrorType.OPEN_LOCK_FAIL.getKey()))
                                                .addHeader("msg", ErrorType.OPEN_LOCK_FAIL.getValue()));
                                    }
                                    break;
                                case "4":
                                    message.reply(null, new DeliveryOptions()
                                            .addHeader("code", String.valueOf(ErrorType.OPEN_LOCK__NOTFAIL.getKey()))
                                            .addHeader("msg", ErrorType.OPEN_LOCK__NOTFAIL.getValue()));
                                    break;
                                default:
                                    message.reply(new JsonObject());
                                    break;
                            }
                        }
                    });
        }
    }

    /**
     * @Description 获取设备列表
     * @author zhang bo
     * @date 17-12-24
     * @version 1.0
     */
    public void getAdminDevlist(Message<JsonObject> message) {
        JsonObject jsonObject = message.body();
        MongoClient.client.findWithOptions("kdsNormalLock", new JsonObject().put("uid", jsonObject.getString("user_id")),
                new FindOptions().setFields(new JsonObject().put("lockName", 1).put("lockNickName", 1).put("is_admin", 1)
                        .put("open_purview", 1).put("auto_lock", 1).put("macLock", 1).put("circle_radius", 1).put("center_latitude", 1)
                        .put("center_longitude", 1).put("password1", 1).put("password2", 1).put("model", 1)), rs -> {
                    if (rs.failed()) {
                        logger.error(rs.cause().getMessage(), rs);
                    } else {
                        if (Objects.nonNull(rs.result()))
                            message.reply(new JsonArray(rs.result()));
                        else
                            message.reply(null);
                    }
                });
    }


    /**
     * @Description 设备下的普通用户列表
     * @author zhang bo
     * @date 17-12-24
     * @version 1.0
     */
    public void getNormalDevlist(Message<JsonObject> message) {
        JsonObject jsonObject = message.body();
        MongoClient.client.findWithOptions("kdsNormalLock", new JsonObject().put("lockName", jsonObject.getString("devname")),
                new FindOptions().setFields(new JsonObject().put("unickname", 1).put("uname", 1).put("datestart", 1)
                        .put("dateend", 1).put("open_purview", 1).put("items", 1).put("adminname", 1)), rs -> {
                    if (rs.failed()) {
                        logger.error(rs.cause().getMessage(), rs);
                    } else {
                        if (Objects.nonNull(rs.result()))
                            message.reply(new JsonArray(rs.result().stream().filter(e -> !e.getString("uname").equals(e.getString("adminname")))
                                    .collect(Collectors.toList())));
                        else
                            message.reply(null);
                    }
                });
    }


    /**
     * @Description 管理员修改锁的位置信息
     * @author zhang bo
     * @date 17-12-24
     * @version 1.0
     */
    public void editAdminDev(Message<JsonObject> message) {
        JsonObject jsonObject = message.body();
        MongoClient.client.updateCollectionWithOptions("kdsNormalLock", new JsonObject().put("lockName", jsonObject.getString("devname"))
                , new JsonObject().put("$set", new JsonObject().put("macLock", jsonObject.getString("devmac"))
                        .put("center_latitude", jsonObject.getString("center_latitude")).put("center_longitude", jsonObject.getString("center_longitude"))
                        .put("edge_latitude", jsonObject.getString("edge_latitude")).put("edge_longitude", jsonObject.getString("edge_longitude"))
                        .put("circle_radius", jsonObject.getString("circle_radius"))), new UpdateOptions().setMulti(true), rs -> {
                    if (rs.failed()) {
                        logger.error(rs.cause().getMessage(), rs);
                    } else {
                        message.reply(new JsonObject());
                    }

                });
    }


    /**
     * @Description 获取设备经纬度等信息
     * @author zhang bo
     * @date 17-12-24
     * @version 1.0
     */
    public void getAdminDevlocklongtitude(Message<JsonObject> message) {
        JsonObject jsonObject = message.body();
        MongoClient.client.findOne("kdsNormalLock", new JsonObject().put("lockName", jsonObject.getString("devname"))
                .put("uid", jsonObject.getString("user_id")), new JsonObject().put("center_latitude", 1).put("center_longitude", 1)
                .put("circle_radius", 1).put("edge_latitude", 1).put("edge_longitude", 1).put("auto_lock", 1).put("open_purview", 1), rs -> {
            if (rs.failed()) {
                logger.error(rs.cause().getMessage(), rs);
            } else {
                message.reply(rs.result());
            }
        });
    }


    /**
     * @Description 修改设备是否开启自动解锁功能
     * @author zhang bo
     * @date 17-12-24
     * @version 1.0
     */
    public void updateAdminDevAutolock(Message<JsonObject> message) {
        JsonObject jsonObject = message.body();
        MongoClient.client.updateCollection("kdsNormalLock", new JsonObject().put("lockName", jsonObject.getString("devname"))
                        .put("uid", jsonObject.getString("user_id"))
                , new JsonObject().put("$set", new JsonObject().put("auto_lock", jsonObject.getString("auto_lock"))), rs -> {
                    if (rs.failed()) {
                        logger.error(rs.cause().getMessage(), rs);
                    } else {
                        message.reply(new JsonObject());
                    }

                });
    }


    /**
     * @Description 修改设备昵称
     * @author zhang bo
     * @date 17-12-24
     * @version 1.0
     */
    public void updateAdminlockNickName(Message<JsonObject> message) {
        JsonObject jsonObject = message.body();
        MongoClient.client.updateCollection("kdsNormalLock", new JsonObject().put("lockName", jsonObject.getString("devname"))
                        .put("uid", jsonObject.getString("user_id"))
                , new JsonObject().put("$set", new JsonObject().put("lockNickName", jsonObject.getString("lockNickName"))), rs -> {
                    if (rs.failed()) {
                        logger.error(rs.cause().getMessage(), rs);
                    } else {
                        message.reply(new JsonObject());
                    }
                });
    }


    /**
     * @Description 检测是否被绑定
     * @author zhang bo
     * @date 17-12-24
     * @version 1.0
     */
    public void checkAdmindev(Message<JsonObject> message) {
        JsonObject jsonObject = message.body();
        MongoClient.client.findOne("kdsNormalLock", new JsonObject().put("lockName", jsonObject.getString("devname"))
                , new JsonObject().put("_id", 1), rs -> {
                    if (rs.failed()) {
                        logger.error(rs.cause().getMessage(), rs);
                    } else {
                        if (Objects.nonNull(rs.result())) {
                            message.reply(rs.result().getString("_id"), new DeliveryOptions()
                                    .addHeader("code", String.valueOf(ErrorType.BINDED.getKey()))
                                    .addHeader("msg", ErrorType.BINDED.getValue()));
                        } else {
                            message.reply(new JsonObject(), new DeliveryOptions()
                                    .addHeader("code", String.valueOf(ErrorType.NOT_BIND.getKey()))
                                    .addHeader("msg", ErrorType.NOT_BIND.getValue()));
                        }
                    }
                });
    }


    /**
     * @Description 上传开门记录
     * @author zhang bo
     * @date 17-12-24
     * @version 1.0
     */
    public void uploadOpenLockList(Message<JsonObject> message) {
        JsonObject jsonObject = message.body();
        RedisClient.client.hget(RedisKeyConf.USER_ACCOUNT + jsonObject.getString("user_id"), RedisKeyConf.USER_VAL_INFO, rs -> {
            if (rs.failed()) {
                logger.error(rs.cause().getMessage(), rs);
            } else {
                JsonObject userInfo = new JsonObject(rs.result());
                String uname = userInfo.getString("username");

                List<BulkOperation> bulkOperations = new ArrayList<>();
                jsonObject.getJsonArray("openLockList").forEach(e -> {
                    JsonObject openLockLists = (JsonObject) e;
                    JsonObject params = new JsonObject().put("type", BulkOperation.BulkOperationType.UPDATE)
                            .put("filter", new JsonObject().put("open_time", openLockLists.getString("open_time"))
                                    .put("open_type", openLockLists.getString("open_type")).put("lockName", jsonObject.getString("device_name")))
                            .put("document", new JsonObject().put("$set", openLockLists.put("uname", uname)
                                    .put("versionType", message.body().getString("versionType"))
//                                    .put("nickName", userInfo.getString("nickName"))
                                    .put("lockName", jsonObject.getString("device_name")).put("lockNickName", jsonObject.getString("device_nickname"))
                                    .put("open_purview", "0"))).put("upsert", true).put("multi", false);
                    if (!Objects.nonNull(openLockLists.getValue("nickName")))
                        params.getJsonObject("document").getJsonObject("$set").put("nickName", openLockLists.getString("user_num"));
                    else
                        params.getJsonObject("document").getJsonObject("$set").put("status", 1);//标志是锁编号昵称映射
                    bulkOperations.add(new BulkOperation(params));
                });
                MongoClient.client.bulkWrite("kdsOpenLockList", bulkOperations, ars -> {
                    if (ars.failed()) logger.error(ars.cause().getMessage(), ars);
                });
                message.reply(new JsonObject());
            }
        });

    }


    /**
     * @Description 修改鎖信息
     * @author zhang bo
     * @date 18-7-11
     * @version 1.0
     */
    @Deprecated
    public void updateLockInfo(Message<JsonObject> message) {
        MongoClient.client.updateCollection("kdsNormalLock", new JsonObject().put("lockName",
                message.body().getString("devname")).put("adminuid", message.body().getString("uid"))
                , new JsonObject().put("lockNickName", message.body().getString("uid")), rs -> {
                    if (rs.failed()) {
                        logger.error(rs.cause().getMessage(), rs);
                        message.reply(null);
                    } else {
                        message.reply(new JsonObject());
                    }
                });
    }


    /**
     * @Description 上傳無服務器鉴权開門記錄 open_purview 1表示1次  2表示多次 3表示永久
     * auth 0 无鉴权
     * @author zhang bo
     * @date 18-7-11
     * @version 1.0
     */
    public void openLockNoAuth(Message<JsonObject> message) {
        MongoClient.client.findOne("kdsNormalLock", new JsonObject().put("lockName", message.body().getString("devname"))
                        .put("uid", message.body().getString("uid")),
                new JsonObject().put("uname", 1).put("lockNickName", 1).put("unickname", 1), ars -> {
                    if (ars.failed()) {
                        logger.error(ars.cause().getMessage(), ars);
                        message.reply(null);
                    } else {
                        MongoClient.client.insert("kdsOpenLockList", new JsonObject().put("lockName", message.body().getString("devname"))
                                .put("versionType", message.body().getString("versionType"))
                                .put("lockNickName", ars.result().getString("lockNickName")).put("nickName", ars.result().getString("unickname"))
                                .put("uname", ars.result().getString("uname")).put("open_purview",
                                        message.body().getString("open_purview"))
                                .put("auth", 0).put("open_time", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()))
                                .put("open_type", message.body().getString("open_type")), rs -> {
                            if (rs.failed()) {
                                logger.error(rs.cause().getMessage(), rs);
                                message.reply(null);
                            } else {
                                message.reply(new JsonObject());
                            }
                        });
                    }
                });
    }


    /**
     * @Description 修改鎖的編號信息
     * @author zhang bo
     * @date 18-7-13
     * @version 1.0
     */
    public void updateLockNumInfo(Message<JsonObject> message) {
        MongoClient.client.updateCollection("kdsDeviceList", new JsonObject().put("lockName", message.body().getString("devname"))
                        .put("uname", message.body().getString("uid")).put("infoList.num", new JsonObject()
                                .put("$in", new JsonArray().add(message.body().getString("num")))),
                new JsonObject().put("$pull", new JsonObject().put("infoList"
                        , new JsonObject().put("num", message.body().getString("num")))), as -> {
                    if (as.failed()) {
                        logger.error(as.cause().getMessage(), as);
                        message.reply(null);
                    } else {
                        MongoClient.client.updateCollectionWithOptions("kdsDeviceList", new JsonObject().put("lockName"
                                , message.body().getString("devname")).put("uname", message.body().getString("uid")),
                                new JsonObject().put("$addToSet", new JsonObject().put("infoList"
                                        , new JsonObject().put("num", message.body().getString("num"))
                                                .put("numNickname", Objects.nonNull(message.body().getValue("numNickname")) ?
                                                        message.body().getString("numNickname") : "")
                                                .put("time", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))))
                                , new UpdateOptions().setUpsert(false), rs -> {
                                    if (rs.failed()) {
                                        logger.error(rs.cause().getMessage(), rs);
                                        message.reply(null);
                                    } else {
                                        message.reply(new JsonObject());
                                    }
                                });
                    }
                });
    }


    /**
     * @Description 批量修改鎖的編號信息
     * @author zhang bo
     * @date 18-8-31
     * @version 1.0
     */
    public void updateBulkLockNumInfo(Message<JsonObject> message) {
        List<BulkOperation> delbulkOperations = new ArrayList<>();
        List<BulkOperation> writeBulkOperations = new ArrayList<>();
        message.body().getJsonArray("infoList").forEach(e -> {
            JsonObject openLockLists = (JsonObject) e;

            JsonObject delParams = new JsonObject().put("type", BulkOperation.BulkOperationType.UPDATE)
                    .put("filter", new JsonObject().put("lockName", message.body().getString("devname"))
                            .put("uname", message.body().getString("uid")).put("infoList.num", new JsonObject()
                                    .put("$in", new JsonArray().add(openLockLists.getString("num")))))
                    .put("document", new JsonObject().put("$pull", new JsonObject().put("infoList"
                            , new JsonObject().put("num", openLockLists.getString("num"))))).put("upsert", false).put("multi", false);
            delbulkOperations.add(new BulkOperation(delParams));


            JsonObject writeParams = new JsonObject().put("type", BulkOperation.BulkOperationType.UPDATE)
                    .put("filter", new JsonObject().put("lockName"
                            , message.body().getString("devname")).put("uname", message.body().getString("uid")))
                    .put("document", new JsonObject().put("$addToSet", new JsonObject().put("infoList"
                            , new JsonObject().put("num", openLockLists.getString("num"))
                                    .put("numNickname", Objects.nonNull(openLockLists.getValue("numNickname")) ?
                                            openLockLists.getString("numNickname") : "")
                                    .put("time", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))))))
                    .put("upsert", false).put("multi", false);
            writeBulkOperations.add(new BulkOperation(writeParams));

        });
        MongoClient.client.bulkWrite("kdsDeviceList", delbulkOperations, ars -> {
            if (ars.failed()) {
                logger.error(ars.cause().getMessage(), ars);
                message.reply(null);
            } else {
                MongoClient.client.bulkWrite("kdsDeviceList", writeBulkOperations, rs -> {
                    if (rs.failed()) {
                        logger.error(rs.cause().getMessage(), rs);
                        message.reply(null);
                    } else {
                        message.reply(new JsonObject());
                    }
                });
            }
        });
    }


    /**
     * @Description 獲取鎖的編號信息
     * @author zhang bo
     * @date 18-7-13
     * @version 1.0
     */
    public void getLockNumInfo(Message<JsonObject> message) {
        MongoClient.client.findWithOptions("kdsDeviceList", new JsonObject().put("lockName", message.body().getString("devname"))
                .put("uname", message.body().getString("uid")), new FindOptions().setFields(new JsonObject().put("infoList", 1)
                .put("_id", 0)), rs -> {
            if (rs.failed()) {
                logger.error(rs.cause().getMessage(), rs);
                message.reply(null);
            } else {
                if (Objects.nonNull(rs.result())) {
                    message.reply(new JsonArray(rs.result()));
                } else {
                    message.reply(new JsonArray());
                }
            }
        });
    }


    /**
     * @Description 查询开锁记录
     * @author zhang bo
     * @date 18-7-13
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void selectOpenLockRecord(Message<JsonObject> message) {
        JsonObject jsonObject = message.body();
        MongoClient.client.findOne("kdsNormalLock", new JsonObject().put("lockName", jsonObject.getString("devname"))
                        .put("adminuid", new JsonObject().put("$exists", true)).put("uid", jsonObject.getString("uid")),
                new JsonObject().put("adminuid", 1).put("_id", 0).put("uname", 1), ars -> {
                    if (ars.failed()) {
                        logger.error(ars.cause().getMessage(), ars);
                        message.reply(null);
                    } else {
                        if (!Objects.nonNull(ars.result())) {
                            message.reply(null, new DeliveryOptions().addHeader("code",
                                    String.valueOf(ErrorType.DEVICE_NOT_FOUND.getKey())).addHeader("msg", ErrorType.DEVICE_NOT_FOUND.getValue()));
                            return;
                        }
                        JsonObject paramsJsonObject = new JsonObject()
                                .put("versionType", message.body().getString("versionType"));
                        // 根据不同权限查询记录
                        if (Objects.nonNull(ars.result()) && ars.result().getString("adminuid").equals(jsonObject.getString("uid")))
                            paramsJsonObject.put("lockName", jsonObject.getString("devname"))
                                    .put("open_time", new JsonObject().put("$gte", message.body().getString("start_time")).put("$lte"
                                            , message.body().getString("end_time")));
                        else
                            paramsJsonObject.put("lockName", jsonObject.getString("devname")).put("uname", ars.result().getString("uname"))
                                    .put("open_time", new JsonObject().put("$gte", message.body().getString("start_time")).put("$lte"
                                            , message.body().getString("end_time")));

                        if (Objects.nonNull(message.body().getValue("content")))
                            paramsJsonObject.put("$or", new JsonArray().add(
                                    new JsonObject().put("nickName", new JsonObject().put("$regex"
                                            , message.body().getString("content")).put("$options", "i")))
                                    .add(new JsonObject().put("user_num", message.body().getString("content"))));//檢索

                        int page = message.body().getInteger("page");
                        int pageNum = message.body().getInteger("pageNum");
                        MongoClient.client.findWithOptions("kdsOpenLockList", paramsJsonObject,
                                new FindOptions().setSort(new JsonObject().put("open_time", -1))
                                        .setLimit(pageNum).setSkip((page - 1) * pageNum), rs -> {
                                    if (rs.failed()) {
                                        logger.error(rs.cause().getMessage(), rs);
                                        message.reply(null);
                                    } else {
                                        if (Objects.nonNull(rs.result())) {
                                            message.reply(new JsonArray(rs.result()));
                                        } else
                                            message.reply(new JsonArray());
                                    }
                                });
                    }
                });
    }


    /**
     * @Description 通过网关开锁
     * @author zhang bo
     * @date 18-7-20
     * @version 1.0
     */
    public void openLockByGateway(Message<JsonObject> message) {
        RedisClient.client.hget(RedisKeyConf.USER_ACCOUNT + message.body().getString("userId"), RedisKeyConf.USER_VAL_INFO, as -> {
            if (as.failed()) {
                logger.error(as.cause().getMessage(), as);
            } else {
                if (Objects.nonNull(as.result())) {
                    JsonObject userInfo = new JsonObject(as.result());
                    MongoClient.client.updateCollectionWithOptions("kdsOpenLockList", new JsonObject()
                                    .put("lockName", message.body().getString("deviceId")).put("uid", message.body().getString("userId"))
                                    .put("medium", message.body().getString("gwId")).put("open_type"
                                    , message.body().getJsonObject("params").getString("type")).put("open_time"
                                    , message.body().getString("timestamp"))
                            , new JsonObject().put("$set", new JsonObject()
                                    .put("open_time", message.body().getString("timestamp"))
                                    .put("open_type", message.body().getJsonObject("params").getString("type"))
                                    .put("medium", message.body().getString("gwId"))
                                    .put("lockName", message.body().getString("deviceId"))
                                    .put("uid", message.body().getString("userId"))
                                    .put("nickName", userInfo.getString("nickName")))
                            , new UpdateOptions().setMulti(false).setUpsert(true), rs -> {
                                if (rs.failed())
                                    logger.error(rs.cause().getMessage(), rs);
                            });
                }
            }
        });

    }

}









