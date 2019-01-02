package cn.orangeiot.apidao.handler.dao.admindev;

import cn.orangeiot.apidao.client.MongoClient;
import cn.orangeiot.apidao.client.RedisClient;
import cn.orangeiot.apidao.conf.RedisKeyConf;
import cn.orangeiot.common.constant.mongodb.*;
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
public class AdminDevDao implements AdminlockAddr, MessageAddr{
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
        MongoClient.client.insert(KdsDeviceList.COLLECT_NAME, new JsonObject().put(KdsDeviceList.LOCK_NAME, jsonObject.getString("devname"))
                .put(KdsDeviceList.U_NAME, jsonObject.getString("user_id")).put(KdsDeviceList.MODEL
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

                        MongoClient.client.findOne(KdsNormalLock.COLLECT_NAME, new JsonObject().put(KdsNormalLock.LOCK_NAME, jsonObject.getString("devname"))
                                , new JsonObject().put(KdsNormalLock._ID, 1), flag -> {
                                    if (flag.failed()) {
                                        logger.error(flag.cause().getMessage(), flag);
                                    } else {
                                        if (!Objects.nonNull(flag.result())) {
                                            MongoClient.client.insert(KdsNormalLock.COLLECT_NAME, new JsonObject().put(KdsNormalLock.LOCK_NAME
                                                    , jsonObject.getString("devname")).put(KdsNormalLock.LOCK_NICK_NAME, jsonObject.getString("devname"))
                                                            .put(KdsNormalLock.VERSION_TYPE, message.body().getString("versionType"))
                                                            .put(KdsNormalLock.MAC_LOCK, finalMacLock).put(KdsNormalLock.ADMIN_UID, jsonObject.getString("user_id"))
                                                            .put(KdsNormalLock.ADMIN_NAME, adminname).put(KdsNormalLock.ADMIN_NICK_NAME, userInfo.getString("nickName"))
                                                            .put(KdsNormalLock.UID, jsonObject.getString("user_id")).put(KdsNormalLock.U_NAME, adminname).put(KdsNormalLock.U_NICK_NAME, userInfo.getString("nickName"))
                                                            .put(KdsNormalLock.OPEN_PURVIEW, "3").put(KdsNormalLock.IS_ADMIN, "1").put(KdsNormalLock.DATE_START, "0").put(KdsNormalLock.DATE_END, "0").put(KdsNormalLock.CENTER_LATITUDE, "0")
                                                            .put(KdsNormalLock.CENTER_LONGITUDE, "0").put(KdsNormalLock.EDGE_LATITUDE, "0").put(KdsNormalLock.EDGE_LONGITUDE, "0").put(KdsNormalLock.CIRCLE_RADIUS, "0").put(KdsNormalLock.AUTO_LOCK, "0")
                                                            .put(KdsNormalLock.ITEMS, new JsonArray().add("0").add("0").add("0").add("0").add("0").add("0").add("0"))
                                                            .put(KdsNormalLock.PASSWORD1, Objects.nonNull(jsonObject.getValue("password1")) ? jsonObject.getString("password1") : "")
                                                            .put(KdsNormalLock.PASSWORD2, Objects.nonNull(jsonObject.getValue("password2")) ? jsonObject.getString("password2") : "")
                                                            .put(KdsNormalLock.MODEL, Objects.nonNull(jsonObject.getValue("model")) ? jsonObject.getString("model") : "")
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
        MongoClient.client.removeDocuments(KdsNormalLock.COLLECT_NAME, new JsonObject().put(KdsNormalLock.LOCK_NAME, jsonObject.getString("devname"))
                , rs -> {
                    if (rs.failed()) logger.error(rs.cause().getMessage(), rs);
                });
        MongoClient.client.removeDocuments(KdsDeviceList.COLLECT_NAME, new JsonObject().put(KdsDeviceList.LOCK_NAME, jsonObject.getString("devname"))
                , rs -> {
                    if (rs.failed()) logger.error(rs.cause().getMessage(), rs);
                });
        MongoClient.client.removeDocuments(KdsOpenLockList.COLLECT_NAME, new JsonObject().put(KdsOpenLockList.LOCK_NAME, jsonObject.getString("devname"))
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
        MongoClient.client.findOne(KdsNormalLock.COLLECT_NAME, new JsonObject().put(KdsNormalLock.LOCK_NAME, jsonObject.getString("devname"))
                        .put(KdsNormalLock.ADMIN_UID, new JsonObject().put("$exists", true)),
                new JsonObject().put(KdsNormalLock.ADMIN_UID, 1), ars -> {
                    if (ars.failed()) {
                        logger.error(ars.cause().getMessage(), ars);
                        message.reply(null);
                    } else {
                        if (Objects.nonNull(ars.result()) && Objects.nonNull(ars.result().getValue(KdsNormalLock.ADMIN_UID))) {
                            message.reply(new JsonObject());
                            if (ars.result().getString(KdsNormalLock.ADMIN_UID)
                                    .equals(jsonObject.getString("adminid"))) {//管理员删除设备
                                MongoClient.client.removeDocuments(KdsNormalLock.COLLECT_NAME, new JsonObject().put(KdsNormalLock.LOCK_NAME, jsonObject.getString("devname"))
                                        , rs -> {
                                            if (rs.failed()) logger.error(rs.cause().getMessage(), rs);
                                        });
                                MongoClient.client.removeDocuments(KdsDeviceList.COLLECT_NAME, new JsonObject().put(KdsDeviceList.LOCK_NAME, jsonObject.getString("devname"))
                                        , rs -> {
                                            if (rs.failed()) logger.error(rs.cause().getMessage(), rs);
                                        });
                                MongoClient.client.removeDocuments(KdsOpenLockList.COLLECT_NAME, new JsonObject().put(KdsOpenLockList.LOCK_NAME, jsonObject.getString("devname"))
//                                                .put("versionType", message.body().getString("versionType"))
                                        , rs -> {
                                            if (rs.failed()) logger.error(rs.cause().getMessage(), rs);
                                        });
                            } else {//普通用户删除设备
                                MongoClient.client.removeDocument(KdsNormalLock.COLLECT_NAME, new JsonObject().put(KdsNormalLock.LOCK_NAME, jsonObject.getString("devname"))
                                                .put(KdsNormalLock.UID, jsonObject.getString("adminid"))
                                        , rs -> {
                                            if (rs.failed()) logger.error(rs.cause().getMessage(), rs);
                                        });
                            }
                        }else{
                            message.reply(null);
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
        MongoClient.client.findOne(KdsUser.COLLECT_NAME, new JsonObject().put(KdsUser.VERSION_TYPE, message.body().getString("versionType"))
                        .put("$or", new JsonArray().add(new JsonObject().put(KdsUser.USER_TEL,
                                jsonObject.getString("dev_username"))).add(new JsonObject().put(KdsUser.USER_MAIL, jsonObject.getString("dev_username")))),
                new JsonObject().put(KdsUser._ID, 1), ars -> {//获取device_username的用户信息
                    if (ars.failed()) {
                        logger.error(ars.cause().getMessage(), ars);
                    } else {
                        MongoClient.client.removeDocument(KdsNormalLock.COLLECT_NAME, new JsonObject().put(KdsNormalLock.LOCK_NAME, jsonObject.getString("devname"))
                                .put(KdsNormalLock.UID, ars.result().getString("_id")), rs -> {
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
        MongoClient.client.findOne(KdsUser.COLLECT_NAME, new JsonObject().put(KdsUser.VERSION_TYPE, message.body().getString("versionType"))
                        .put("$or", new JsonArray().add(new JsonObject().put(KdsUser.USER_TEL
                                , jsonObject.getString("device_username"))).add(new JsonObject().put(KdsUser.USER_MAIL, jsonObject.getString("device_username")))),
                new JsonObject().put(KdsUser.NICK_NAME, 1).put(KdsUser.USER_MAIL, 1).put(KdsUser.USER_TEL, 1), ars -> {//获取device_username的用户信息
                    if (ars.failed()) {
                        logger.error(ars.cause().getMessage(), ars);
                    } else {
                        if (Objects.nonNull(ars.result())) {
                            MongoClient.client.findOne(KdsNormalLock.COLLECT_NAME, new JsonObject().put(KdsNormalLock.ADMIN_UID, jsonObject.getString("admin_id")).put(KdsNormalLock.LOCK_NAME, jsonObject.getString("devname"))
                                    , new JsonObject(), returnData -> {
                                        if (returnData.failed()) {
                                            logger.error(returnData.cause().getMessage(), returnData);
                                        } else {
                                            if (Objects.nonNull(returnData.result())) {//是否是锁的管理员
                                                String adminname = returnData.result().getString(KdsNormalLock.ADMIN_NAME);
                                                String uname = "";
                                                if (Objects.nonNull(ars.result().getValue(KdsUser.USER_MAIL)))
                                                    uname = ars.result().getString(KdsUser.USER_MAIL);
                                                else
                                                    uname = ars.result().getString(KdsUser.USER_TEL);
                                                JsonObject paramsJsonObject = new JsonObject().put(KdsNormalLock.LOCK_NAME
                                                        , jsonObject.getString("devname")).put(KdsNormalLock.LOCK_NICK_NAME, finalLockNickName)
                                                        .put(KdsNormalLock.MAC_LOCK, finalMacLock).put(KdsNormalLock.ADMIN_UID, jsonObject.getString("admin_id"))
                                                        .put(KdsNormalLock.ADMIN_NAME, adminname).put(KdsNormalLock.ADMIN_NICK_NAME, returnData.result().getString(KdsNormalLock.ADMIN_NICK_NAME))
                                                        .put(KdsNormalLock.UID, ars.result().getString("_id")).put(KdsNormalLock.U_NAME, uname).put(KdsNormalLock.U_NICK_NAME, ars.result().getString("nickName"))
                                                        .put(KdsNormalLock.OPEN_PURVIEW, jsonObject.getString("open_purview")).put(KdsNormalLock.IS_ADMIN, "0").put(KdsNormalLock.DATE_START, jsonObject.getString("start_time"))
                                                        .put(KdsNormalLock.DATE_END, jsonObject.getString("end_time")).put(KdsNormalLock.AUTO_LOCK, "0").put(KdsNormalLock.ITEMS, jsonObject.getJsonArray("items"))
                                                        .put(KdsNormalLock.PASSWORD1, Objects.nonNull(returnData.result().getValue("password1")) ? returnData.result().getString("password1") : "")
                                                        .put(KdsNormalLock.PASSWORD2, Objects.nonNull(returnData.result().getValue("password2")) ? returnData.result().getString("password2") : "")
                                                        .put(KdsNormalLock.VERSION_TYPE, message.body().getString("versionType"))
                                                        .put(KdsNormalLock.MODEL, Objects.nonNull(returnData.result().getValue("model")) ? returnData.result().getString("model") : "");

                                                // 查询是否已经添加过的账号
                                                MongoClient.client.findOne(KdsNormalLock.COLLECT_NAME, new JsonObject().put(KdsNormalLock.LOCK_NAME, jsonObject.getString("devname"))
                                                        .put(KdsNormalLock.U_NAME, jsonObject.getString("device_username")), new JsonObject().put(KdsNormalLock._ID, 1), drs -> {
                                                    if (drs.failed()) {
                                                        logger.error(drs.cause().getMessage(), drs);
                                                    } else {
                                                        if (!Objects.nonNull(drs.result())) {
                                                            if (jsonObject.getString("open_purview").equals("3")) {//查詢設備信息
                                                                MongoClient.client.findOne(KdsNormalLock.COLLECT_NAME, new JsonObject().put(KdsNormalLock.LOCK_NAME, jsonObject.getString("devname"))
                                                                        .put(KdsNormalLock.UID, jsonObject.getString("admin_id")), new JsonObject().put(KdsNormalLock.CENTER_LATITUDE, 1)
                                                                        .put(KdsNormalLock.CENTER_LONGITUDE, 1).put(KdsNormalLock.EDGE_LATITUDE, 1).put(KdsNormalLock.EDGE_LONGITUDE, 1).put(KdsNormalLock.CIRCLE_RADIUS, 1), rs -> {
                                                                    if (rs.failed()) {
                                                                        logger.error(rs.cause().getMessage(), rs);
                                                                    } else {
                                                                        paramsJsonObject.put(KdsNormalLock.CENTER_LATITUDE, rs.result().getString("center_latitude"))
                                                                                .put(KdsNormalLock.CENTER_LONGITUDE, rs.result().getString("center_longitude")).put(KdsNormalLock.EDGE_LATITUDE, rs.result().getString("edge_latitude"))
                                                                                .put(KdsNormalLock.EDGE_LONGITUDE, rs.result().getString("edge_longitude")).put(KdsNormalLock.CIRCLE_RADIUS, rs.result().getString("circle_radius"));
                                                                        MongoClient.client.insert(KdsNormalLock.COLLECT_NAME, paramsJsonObject, res -> {
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
                                                                paramsJsonObject.put(KdsNormalLock.CENTER_LATITUDE, "0")
                                                                        .put(KdsNormalLock.CENTER_LONGITUDE, "0").put(KdsNormalLock.EDGE_LATITUDE, "0").put(KdsNormalLock.EDGE_LONGITUDE, "0").put(KdsNormalLock.CIRCLE_RADIUS, "0");
                                                                MongoClient.client.insert(KdsNormalLock.COLLECT_NAME, paramsJsonObject, res -> {
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
        MongoClient.client.findOne(KdsNormalLock.COLLECT_NAME, new JsonObject().put(KdsNormalLock.LOCK_NAME, jsonObject.getString("device_name"))
                        .put(KdsNormalLock.ADMIN_UID, new JsonObject().put("$exists", true)).put(KdsNormalLock.UID, jsonObject.getString("user_id")),
                new JsonObject().put(KdsNormalLock.ADMIN_UID, 1).put(KdsNormalLock._ID, 0).put(KdsNormalLock.U_NAME, 1), ars -> {
                    if (ars.failed()) {
                        logger.error(ars.cause().getMessage(), ars);
                        message.reply(null);
                    } else {
                        JsonObject paramsJsonObject = new JsonObject()
                                .put(KdsNormalLock.VERSION_TYPE, message.body().getString("versionType"));
                        // 根据不同权限查询记录
                        if (Objects.nonNull(ars.result()) && ars.result().getString("adminuid").equals(jsonObject.getString("user_id")))
                            paramsJsonObject.put(KdsNormalLock.LOCK_NAME, jsonObject.getString("device_name"));
                        else
                            paramsJsonObject.put(KdsNormalLock.LOCK_NAME, jsonObject.getString("device_name")).put(KdsNormalLock.U_NAME, ars.result().getString("uname"));

                        MongoClient.client.findWithOptions(KdsOpenLockList.COLLECT_NAME, paramsJsonObject,
                                new FindOptions().setLimit(20).setSkip((pageNum - 1) * 20).setSort(
                                        new JsonObject().put(KdsOpenLockList.OPEN_TIME, -1)), rs -> {
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
        JsonObject paramsJsonObject = new JsonObject().put("$set", new JsonObject().put(KdsNormalLock.DATE_START, Objects.nonNull(jsonObject.getValue("datestart"))
                ? jsonObject.getString("datestart") : "0")
                .put(KdsNormalLock.DATE_END, Objects.nonNull(jsonObject.getValue("dateend"))
                        ? jsonObject.getString("dateend") : "0").put(KdsNormalLock.OPEN_PURVIEW, jsonObject.getString("open_purview"))
                .put(KdsNormalLock.ITEMS, Objects.nonNull(jsonObject.getValue("items")) ? jsonObject.getJsonArray("items")
                        : new JsonArray().add("0").add("0").add("0").add("0").add("0").add("0").add("0")));
        MongoClient.client.updateCollection(KdsNormalLock.COLLECT_NAME, new JsonObject().put(KdsNormalLock.U_NAME, jsonObject.getString("dev_username"))
                .put(KdsNormalLock.LOCK_NAME, jsonObject.getString("devname")).put(KdsNormalLock.ADMIN_UID, jsonObject.getString("admin_id")), paramsJsonObject, rs -> {
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
            MongoClient.client.findOne(KdsNormalLock.COLLECT_NAME, new JsonObject().put(KdsNormalLock.LOCK_NAME, jsonObject.getString("devname"))
                            .put(KdsNormalLock.ADMIN_UID, new JsonObject().put("$exists", true)),
                    new JsonObject().put(KdsNormalLock.ADMIN_NAME, 1).put(KdsNormalLock.LOCK_NICK_NAME, 1).put(KdsNormalLock.ADMIN_NICK_NAME, 1), ars -> {
                        if (ars.failed()) {
                            logger.error(ars.cause().getMessage(), ars);
                        } else {
                            if (Objects.nonNull(ars.result())) {
                                if (!jsonObject.getString("open_type").equals("100")) {//不上傳開鎖記錄，會重復
                                    MongoClient.client.insert(KdsOpenLockList.COLLECT_NAME, new JsonObject().put(KdsOpenLockList.LOCK_NAME, jsonObject.getString("devname"))
                                            .put(KdsOpenLockList.VERSION_TYPE, message.body().getString("versionType"))
                                            .put(KdsOpenLockList.LOCK_NICK_NAME, ars.result().getString("lockNickName"))
                                            .put(KdsOpenLockList.NICK_NAME, Objects.nonNull(message.body().getValue("nickName")) ?
                                                    message.body().getValue("nickName") : ars.result().getString("adminnickname"))
                                            .put(KdsOpenLockList.U_NAME, ars.result().getString("adminname")).put("open_purview", "3").put(KdsOpenLockList.OPEN_TIME, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()))
                                            .put(KdsOpenLockList.OPEN_TYPE, jsonObject.getString("open_type")), rs -> {
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
            MongoClient.client.findOne(KdsNormalLock.COLLECT_NAME, new JsonObject().put(KdsNormalLock.LOCK_NAME, jsonObject.getString("devname")).put(KdsNormalLock.UID, jsonObject.getString("user_id"))
                    , new JsonObject().put(KdsNormalLock.U_NAME, 1).put(KdsNormalLock.LOCK_NICK_NAME, 1).put(KdsNormalLock.U_NICK_NAME, 1).put(KdsNormalLock.OPEN_PURVIEW, 1)
                            .put(KdsNormalLock.DATE_START, 1).put(KdsNormalLock.DATE_END, 1).put(KdsNormalLock.ITEMS, 1), ars -> {
                        if (ars.failed()) {
                            logger.error(ars.cause().getMessage(), ars);
                        } else {
                            JsonObject paramsJsoNObject = new JsonObject().put(KdsNormalLock.LOCK_NAME, jsonObject.getString("devname"))
                                    .put(KdsNormalLock.VERSION_TYPE, message.body().getString("versionType"))
                                    .put(KdsNormalLock.LOCK_NICK_NAME, ars.result().getString("lockNickName"))
                                    .put(KdsOpenLockList.NICK_NAME, Objects.nonNull(message.body().getValue("nickName")) ?
                                            message.body().getValue("nickName") : ars.result().getString("unickname"))
                                    .put(KdsNormalLock.U_NAME, ars.result().getString("uname")).put("open_type", jsonObject.getString("open_type"));

                            switch (ars.result().getString("open_purview")) {
                                case "1":
                                    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");//设置日期格式
                                    long todaytime = new Date().getTime();
                                    try {
                                        if (todaytime > df.parse(ars.result().getString("datestart")).getTime() && todaytime < df.parse(ars.result().getString("dateend")).getTime()) {
                                            paramsJsoNObject.put("open_purview", "1").put(KdsOpenLockList.OPEN_TIME, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
                                            if (!jsonObject.getString("open_type").equals("100")) {//不上傳開鎖記錄，會重復
                                                MongoClient.client.insert(KdsOpenLockList.COLLECT_NAME, paramsJsoNObject, rs -> {
                                                    if (rs.failed()) logger.error(rs.cause().getMessage(), rs);
                                                });
                                            }
                                            MongoClient.client.updateCollection(KdsNormalLock.COLLECT_NAME, new JsonObject().put("uid", jsonObject.getString("user_id"))
                                                            .put(KdsNormalLock.LOCK_NAME, jsonObject.getString("devname")), new JsonObject().put("$set", new JsonObject().put(KdsNormalLock.OPEN_PURVIEW, "4"))
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
                                                paramsJsoNObject.put(KdsOpenLockList.OPEN_PURVIEW, "2").put(KdsOpenLockList.OPEN_TIME, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
                                                if (!jsonObject.getString("open_type").equals("100")) {//不上傳開鎖記錄，會重復
                                                    MongoClient.client.insert(KdsOpenLockList.COLLECT_NAME, paramsJsoNObject, rs -> {
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
                                    paramsJsoNObject.put(KdsOpenLockList.OPEN_PURVIEW, "3").put(KdsOpenLockList.OPEN_TIME, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
                                    if (!jsonObject.getString("open_type").equals("100")) {//不上傳開鎖記錄，會重復
                                        MongoClient.client.insert(KdsOpenLockList.COLLECT_NAME, paramsJsoNObject, rs -> {
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
            MongoClient.client.findOne(KdsNormalLock.COLLECT_NAME, new JsonObject().put(KdsNormalLock.LOCK_NAME, jsonObject.getString("devname"))
                            .put(KdsNormalLock.ADMIN_UID, new JsonObject().put("$exists", true)),
                    new JsonObject().put(KdsNormalLock.ADMIN_NAME, 1).put(KdsNormalLock.LOCK_NICK_NAME, 1).put(KdsNormalLock.ADMIN_NICK_NAME, 1), ars -> {
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
            MongoClient.client.findOne(KdsNormalLock.COLLECT_NAME, new JsonObject().put(KdsNormalLock.LOCK_NAME, jsonObject.getString("devname")).put(KdsNormalLock.UID, jsonObject.getString("user_id"))
                    , new JsonObject().put(KdsNormalLock.U_NAME, 1).put(KdsNormalLock.LOCK_NICK_NAME, 1).put(KdsNormalLock.U_NICK_NAME, 1).put(KdsNormalLock.OPEN_PURVIEW, 1)
                            .put(KdsNormalLock.DATE_START, 1).put(KdsNormalLock.DATE_END, 1).put(KdsNormalLock.ITEMS, 1), ars -> {
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
        MongoClient.client.findWithOptions(KdsNormalLock.COLLECT_NAME, new JsonObject().put(KdsNormalLock.UID, jsonObject.getString("user_id")),
                new FindOptions().setFields(new JsonObject().put(KdsNormalLock.LOCK_NAME, 1).put(KdsNormalLock.LOCK_NICK_NAME, 1).put(KdsNormalLock.IS_ADMIN, 1)
                        .put(KdsNormalLock.OPEN_PURVIEW, 1).put(KdsNormalLock.AUTO_LOCK, 1).put(KdsNormalLock.MAC_LOCK, 1).put(KdsNormalLock.CIRCLE_RADIUS, 1).put(KdsNormalLock.CENTER_LATITUDE, 1)
                        .put(KdsNormalLock.CENTER_LONGITUDE, 1).put(KdsNormalLock.PASSWORD1, 1).put(KdsNormalLock.PASSWORD2, 1).put(KdsNormalLock.MODEL, 1)), rs -> {
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
        MongoClient.client.findWithOptions(KdsNormalLock.COLLECT_NAME, new JsonObject().put(KdsNormalLock.LOCK_NAME, jsonObject.getString("devname")),
                new FindOptions().setFields(new JsonObject().put(KdsNormalLock.U_NICK_NAME, 1).put(KdsNormalLock.U_NAME, 1).put(KdsNormalLock.DATE_START, 1)
                        .put(KdsNormalLock.DATE_END, 1).put(KdsNormalLock.OPEN_PURVIEW, 1).put(KdsNormalLock.ITEMS, 1).put(KdsNormalLock.ADMIN_NAME, 1)), rs -> {
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
        MongoClient.client.updateCollectionWithOptions(KdsNormalLock.COLLECT_NAME, new JsonObject().put(KdsNormalLock.LOCK_NAME, jsonObject.getString("devname"))
                , new JsonObject().put("$set", new JsonObject().put(KdsNormalLock.MAC_LOCK, jsonObject.getString("devmac"))
                        .put(KdsNormalLock.CENTER_LATITUDE, jsonObject.getString("center_latitude")).put(KdsNormalLock.CENTER_LONGITUDE, jsonObject.getString("center_longitude"))
                        .put(KdsNormalLock.EDGE_LATITUDE, jsonObject.getString("edge_latitude")).put(KdsNormalLock.EDGE_LATITUDE, jsonObject.getString("edge_longitude"))
                        .put(KdsNormalLock.CIRCLE_RADIUS, jsonObject.getString("circle_radius"))), new UpdateOptions().setMulti(true), rs -> {
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
        MongoClient.client.findOne(KdsNormalLock.COLLECT_NAME, new JsonObject().put(KdsNormalLock.LOCK_NAME, jsonObject.getString("devname"))
                .put(KdsNormalLock.UID, jsonObject.getString("user_id")), new JsonObject().put(KdsNormalLock.CENTER_LATITUDE, 1).put(KdsNormalLock.CENTER_LONGITUDE, 1)
                .put(KdsNormalLock.CIRCLE_RADIUS, 1).put(KdsNormalLock.EDGE_LATITUDE, 1).put(KdsNormalLock.EDGE_LONGITUDE, 1).put(KdsNormalLock.AUTO_LOCK, 1).put(KdsNormalLock.OPEN_PURVIEW, 1), rs -> {
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
        MongoClient.client.updateCollection(KdsNormalLock.COLLECT_NAME, new JsonObject().put(KdsNormalLock.LOCK_NAME, jsonObject.getString("devname"))
                        .put("uid", jsonObject.getString("user_id"))
                , new JsonObject().put("$set", new JsonObject().put(KdsNormalLock.AUTO_LOCK, jsonObject.getString("auto_lock"))), rs -> {
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
        MongoClient.client.updateCollection(KdsNormalLock.COLLECT_NAME, new JsonObject().put(KdsNormalLock.LOCK_NAME, jsonObject.getString("devname"))
                        .put(KdsNormalLock.UID, jsonObject.getString("user_id"))
                , new JsonObject().put("$set", new JsonObject().put(KdsNormalLock.LOCK_NICK_NAME, jsonObject.getString("lockNickName"))), rs -> {
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
        MongoClient.client.findOne(KdsNormalLock.COLLECT_NAME, new JsonObject().put(KdsNormalLock.LOCK_NAME, jsonObject.getString("devname"))
                , new JsonObject().put(KdsNormalLock._ID, 1), rs -> {
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
        if (jsonObject.getJsonArray("openLockList").size() <= 0) {
            message.reply(new JsonObject());
            return;
        }
        RedisClient.client.hget(RedisKeyConf.USER_ACCOUNT + jsonObject.getString("user_id"), RedisKeyConf.USER_VAL_INFO, rs -> {
            if (rs.failed()) {
                logger.error(rs.cause().getMessage(), rs);
                message.reply(null);
            } else {
                JsonObject userInfo = new JsonObject(rs.result());
                String uname = userInfo.getString("username");

                List<BulkOperation> bulkOperations = new ArrayList<>();
                jsonObject.getJsonArray("openLockList").forEach(e -> {
                    JsonObject openLockLists = (JsonObject) e;
                    JsonObject params = new JsonObject().put("type", BulkOperation.BulkOperationType.UPDATE)
                            .put("filter", new JsonObject().put(KdsOpenLockList.OPEN_TIME, openLockLists.getString("open_time"))
                                    .put(KdsOpenLockList.OPEN_TYPE, openLockLists.getString("open_type")).put(KdsOpenLockList.LOCK_NAME, jsonObject.getString("device_name")))
                            .put("document", new JsonObject().put("$set", openLockLists.put(KdsOpenLockList.U_NAME, uname)
                                    .put(KdsOpenLockList.VERSION_TYPE, message.body().getString("versionType"))
//                                    .put("nickName", userInfo.getString("nickName"))
                                    .put(KdsOpenLockList.LOCK_NAME, jsonObject.getString("device_name")).put(KdsOpenLockList.LOCK_NICK_NAME, jsonObject.getString("device_nickname"))
                                    .put(KdsOpenLockList.OPEN_PURVIEW, "0"))).put("upsert", true).put("multi", false);
                    if (!Objects.nonNull(openLockLists.getValue("nickName")))
                        params.getJsonObject("document").getJsonObject("$set").put(KdsOpenLockList.NICK_NAME, openLockLists.getString("user_num"));
                    else
                        params.getJsonObject("document").getJsonObject("$set").put("status", 1);//标志是锁编号昵称映射
                    bulkOperations.add(new BulkOperation(params));
                });
                MongoClient.client.bulkWrite(KdsOpenLockList.COLLECT_NAME, bulkOperations, ars -> {
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
        MongoClient.client.updateCollection(KdsNormalLock.COLLECT_NAME, new JsonObject().put(KdsNormalLock.LOCK_NAME,
                message.body().getString("devname")).put(KdsNormalLock.ADMIN_UID, message.body().getString("uid"))
                , new JsonObject().put(KdsNormalLock.LOCK_NICK_NAME, message.body().getString("uid")), rs -> {
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
        MongoClient.client.findOne(KdsNormalLock.COLLECT_NAME, new JsonObject().put(KdsNormalLock.LOCK_NAME, message.body().getString("devname"))
                        .put(KdsNormalLock.UID, message.body().getString("uid")),
                new JsonObject().put(KdsNormalLock.U_NAME, 1).put(KdsNormalLock.LOCK_NICK_NAME, 1).put(KdsNormalLock.U_NICK_NAME, 1), ars -> {
                    if (ars.failed()) {
                        logger.error(ars.cause().getMessage(), ars);
                        message.reply(null);
                    } else {
                        MongoClient.client.insert(KdsOpenLockList.COLLECT_NAME, new JsonObject().put(KdsOpenLockList.LOCK_NAME, message.body().getString("devname"))
                                .put(KdsOpenLockList.VERSION_TYPE, message.body().getString("versionType"))
                                .put(KdsOpenLockList.LOCK_NICK_NAME, ars.result().getString("lockNickName")).put(KdsOpenLockList.NICK_NAME, ars.result().getString("unickname"))
                                .put(KdsOpenLockList.U_NAME, ars.result().getString("uname")).put(KdsOpenLockList.OPEN_PURVIEW,
                                        message.body().getString("open_purview"))
                                .put(KdsOpenLockList.AUTH, 0).put(KdsOpenLockList.OPEN_TIME, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()))
                                .put(KdsOpenLockList.OPEN_TYPE, message.body().getString("open_type")), rs -> {
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
        MongoClient.client.updateCollection(KdsDeviceList.COLLECT_NAME, new JsonObject().put(KdsDeviceList.LOCK_NAME, message.body().getString("devname"))
                        .put(KdsDeviceList.U_NAME, message.body().getString("uid")).put("infoList.num", new JsonObject()
                                .put("$in", new JsonArray().add(message.body().getString("num")))),
                new JsonObject().put("$pull", new JsonObject().put(KdsDeviceList.INFO_LIST
                        , new JsonObject().put(KdsDeviceList.INFO_LIST_NUM, message.body().getString("num")))), as -> {
                    if (as.failed()) {
                        logger.error(as.cause().getMessage(), as);
                        message.reply(null);
                    } else {
                        MongoClient.client.updateCollectionWithOptions(KdsDeviceList.COLLECT_NAME, new JsonObject().put(KdsDeviceList.LOCK_NAME
                                , message.body().getString("devname")).put(KdsDeviceList.U_NAME, message.body().getString("uid")),
                                new JsonObject().put("$addToSet", new JsonObject().put(KdsDeviceList.INFO_LIST
                                        , new JsonObject().put(KdsDeviceList.INFO_LIST_NUM, message.body().getString("num"))
                                                .put(KdsDeviceList.INFO_LIST_NUM_NICK_NAME, Objects.nonNull(message.body().getValue("numNickname")) ?
                                                        message.body().getString("numNickname") : "")
                                                .put(KdsDeviceList.INFO_LIST_TIME, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))))
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
                    .put("filter", new JsonObject().put(KdsDeviceList.LOCK_NAME, message.body().getString("devname"))
                            .put("uname", message.body().getString("uid")).put("infoList.num", new JsonObject()
                                    .put("$in", new JsonArray().add(openLockLists.getString("num")))))
                    .put("document", new JsonObject().put("$pull", new JsonObject().put("infoList"
                            , new JsonObject().put("num", openLockLists.getString("num"))))).put("upsert", false).put("multi", false);
            delbulkOperations.add(new BulkOperation(delParams));


            JsonObject writeParams = new JsonObject().put("type", BulkOperation.BulkOperationType.UPDATE)
                    .put("filter", new JsonObject().put(KdsDeviceList.LOCK_NAME
                            , message.body().getString("devname")).put("uname", message.body().getString("uid")))
                    .put("document", new JsonObject().put("$addToSet", new JsonObject().put("infoList"
                            , new JsonObject().put("num", openLockLists.getString("num"))
                                    .put("numNickname", Objects.nonNull(openLockLists.getValue("numNickname")) ?
                                            openLockLists.getString("numNickname") : "")
                                    .put("time", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))))))
                    .put("upsert", false).put("multi", false);
            writeBulkOperations.add(new BulkOperation(writeParams));

        });
        MongoClient.client.bulkWrite(KdsDeviceList.COLLECT_NAME, delbulkOperations, ars -> {
            if (ars.failed()) {
                logger.error(ars.cause().getMessage(), ars);
                message.reply(null);
            } else {
                MongoClient.client.bulkWrite(KdsDeviceList.COLLECT_NAME, writeBulkOperations, rs -> {
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
        MongoClient.client.findWithOptions(KdsDeviceList.COLLECT_NAME, new JsonObject().put(KdsDeviceList.LOCK_NAME, message.body().getString("devname"))
                .put(KdsDeviceList.U_NAME, message.body().getString("uid")), new FindOptions().setFields(new JsonObject().put("infoList", 1)
                .put(KdsDeviceList._ID, 0)), rs -> {
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
        MongoClient.client.findOne(KdsNormalLock.COLLECT_NAME, new JsonObject().put(KdsNormalLock.LOCK_NAME, jsonObject.getString("devname"))
                        .put(KdsNormalLock.ADMIN_UID, new JsonObject().put("$exists", true)).put(KdsNormalLock.UID, jsonObject.getString("uid")),
                new JsonObject().put(KdsNormalLock.ADMIN_UID, 1).put(KdsNormalLock._ID, 0).put(KdsNormalLock.U_NAME, 1), ars -> {
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
                                .put(KdsOpenLockList.VERSION_TYPE, message.body().getString("versionType"));
                        // 根据不同权限查询记录
                        if (Objects.nonNull(ars.result()) && ars.result().getString("adminuid").equals(jsonObject.getString("uid")))
                            paramsJsonObject.put(KdsOpenLockList.LOCK_NAME, jsonObject.getString("devname"))
                                    .put(KdsOpenLockList.OPEN_TIME, new JsonObject().put("$gte", message.body().getString("start_time")).put("$lte"
                                            , message.body().getString("end_time")));
                        else
                            paramsJsonObject.put(KdsOpenLockList.LOCK_NAME, jsonObject.getString("devname")).put(KdsOpenLockList.U_NAME, ars.result().getString("uname"))
                                    .put(KdsOpenLockList.OPEN_TIME, new JsonObject().put("$gte", message.body().getString("start_time")).put("$lte"
                                            , message.body().getString("end_time")));

                        if (Objects.nonNull(message.body().getValue("content")))
                            paramsJsonObject.put("$or", new JsonArray().add(
                                    new JsonObject().put(KdsOpenLockList.NICK_NAME, new JsonObject().put("$regex"
                                            , message.body().getString("content")).put("$options", "i")))
                                    .add(new JsonObject().put(KdsOpenLockList.USER_NUM, message.body().getString("content"))));//檢索

                        int page = message.body().getInteger("page");
                        int pageNum = message.body().getInteger("pageNum");
                        MongoClient.client.findWithOptions(KdsOpenLockList.COLLECT_NAME, paramsJsonObject,
                                new FindOptions().setSort(new JsonObject().put(KdsOpenLockList.OPEN_TIME, -1))
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
                    MongoClient.client.findOne(KdsGatewayDeviceList.COLLECT_NAME, new JsonObject().put(KdsGatewayDeviceList.DEVICE_SN, message.body().getString("gwId"))
                            .put(KdsGatewayDeviceList.IS_ADMIN, 1), new JsonObject().put(KdsGatewayDeviceList.UID, 1).put(KdsGatewayDeviceList._ID, 0), ars -> {//獲取管理员
                        if (ars.failed()) {
                            logger.error(ars.cause().getMessage(), ars);
                        } else {
                            if (Objects.nonNull(ars.result()) && Objects.nonNull(ars.result().getValue("uid"))) {//存在管理员
                                JsonObject userInfo = new JsonObject(as.result());
                                MongoClient.client.updateCollectionWithOptions(KdsOpenLockList.COLLECT_NAME, new JsonObject()
                                                .put(KdsOpenLockList.LOCK_NAME, message.body().getString("deviceId")).put(KdsOpenLockList.UID, message.body().getString("userId"))
                                                .put(KdsOpenLockList.MEDIUM, message.body().getString("gwId")).put(KdsOpenLockList.OPEN_TYPE
                                                , message.body().getJsonObject("params").getString("type")).put(KdsOpenLockList.OPEN_TIME
                                                , message.body().getString("timestamp"))
                                        , new JsonObject().put("$set", new JsonObject()
                                                .put(KdsOpenLockList.OPEN_TIME, message.body().getString("timestamp"))
                                                .put(KdsOpenLockList.OPEN_TYPE, message.body().getJsonObject("params").getString("type"))
                                                .put(KdsOpenLockList.MEDIUM, message.body().getString("gwId"))
                                                .put(KdsOpenLockList.LOCK_NAME, message.body().getString("deviceId"))
                                                .put(KdsOpenLockList.UID, message.body().getString("userId"))
                                                .put(KdsOpenLockList.NICK_NAME, userInfo.getString("nickName"))
                                                .put(KdsOpenLockList.ADMIN_UID, ars.result().getString("uid")))
                                        , new UpdateOptions().setMulti(false).setUpsert(true), rs -> {
                                            if (rs.failed())
                                                logger.error(rs.cause().getMessage(), rs);
                                        });
                            } else {
                                logger.warn("openLockByGateway no admin");
                            }
                        }
                    });

                }
            }
        });

    }

}









