package cn.orangeiot.http.handler.lock;

import cn.orangeiot.common.genera.ErrorType;
import cn.orangeiot.common.genera.Result;
import cn.orangeiot.common.options.SendOptions;
import cn.orangeiot.common.utils.DataType;
import cn.orangeiot.http.verify.VerifyParamsUtil;
import cn.orangeiot.reg.adminlock.AdminlockAddr;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import scala.util.parsing.json.JSONArray;

import javax.xml.crypto.Data;
import java.util.List;
import java.util.Objects;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2017-12-26
 */
public class LockHandler implements AdminlockAddr {

    private static Logger logger = LogManager.getLogger(LockHandler.class);

    private EventBus eventBus;

    private JsonObject config;

    public LockHandler(EventBus eventBus, JsonObject config) {
        this.eventBus = eventBus;
        this.config = config;
    }


    /**
     * @Description 添加设备
     * @author zhang bo
     * @date 17-12-26
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void createAdminDev(RoutingContext routingContext) {
        logger.info("==LockHandler=createAdminDev==params->" + routingContext.getBodyAsString());
        //验证参数的合法性
        VerifyParamsUtil.verifyParams(routingContext, new JsonObject().put("devmac", DataType.STRING)
                .put("devname", DataType.STRING).put("user_id", DataType.STRING), asyncResult -> {
            if (asyncResult.failed()) {
                routingContext.fail(401);
            } else {
                Result<JsonObject> result = new Result<>();
                eventBus.send(AdminlockAddr.class.getName() + CREATE_ADMIN_DEV, asyncResult.result(), SendOptions.getInstance()
                        , (AsyncResult<Message<JsonObject>> rs) -> {
                            if (rs.failed()) {
                                routingContext.fail(501);
                            } else {
                                if (Objects.nonNull(rs.result().body())) {
                                    result.setData(null);
                                    routingContext.response().end(JsonObject.mapFrom(result).toString());
                                } else {
                                    if (!rs.result().headers().isEmpty())
                                        routingContext.response().end(JsonObject.mapFrom(
                                                result.setErrorMessage(Integer.parseInt(rs.result().headers().get("code")), rs.result().headers().get("msg"))).toString());
                                    else
                                        routingContext.response().end(JsonObject.mapFrom(
                                                result.setErrorMessage(ErrorType.DEV_REGED.getKey(), ErrorType.DEV_REGED.getValue())).toString());
                                }
                            }
                        });
            }
        });
    }


    /**
     * @Description 重置解绑
     * @author zhang bo
     * @date 17-12-26
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void deletevendorDev(RoutingContext routingContext) {
        logger.info("==LockHandler=deletevendorDev==params->" + routingContext.getBodyAsString());
        //验证参数的合法性
        VerifyParamsUtil.verifyParams(routingContext, new JsonObject().put("adminid", DataType.STRING)
                .put("devname", DataType.STRING), asyncResult -> {
            if (asyncResult.failed()) {
                routingContext.fail(401);
            } else {
                eventBus.send(AdminlockAddr.class.getName() + DELETE_EVEND_DEV, asyncResult.result(), SendOptions.getInstance()
                        , (AsyncResult<Message<JsonObject>> rs) -> {
                            if (rs.failed()) {
                                routingContext.fail(501);
                            } else {
                                routingContext.response().end(JsonObject.mapFrom(new Result<String>()).toString());
                            }
                        });
            }
        });
    }


    /**
     * @Description 用户主动删除设备
     * @author zhang bo
     * @date 17-12-26
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void deleteAdminDev(RoutingContext routingContext) {
        logger.info("==LockHandler=deleteAdminDev==params->" + routingContext.getBodyAsString());
        //验证参数的合法性
        VerifyParamsUtil.verifyParams(routingContext, new JsonObject().put("devname", DataType.STRING)
                .put("adminid", DataType.STRING), asyncResult -> {
            if (asyncResult.failed()) {
                routingContext.fail(401);
            } else {
                eventBus.send(AdminlockAddr.class.getName() + DELETE_ADMIN_DEV, asyncResult.result(), SendOptions.getInstance()
                        , (AsyncResult<Message<JsonObject>> rs) -> {
                            if (rs.failed()) {
                                routingContext.fail(501);
                            } else {
                                routingContext.response().end(JsonObject.mapFrom(new Result<String>()).toString());
                            }
                        });
            }
        });
    }


    /**
     * @Description 管理员删除用户
     * @author zhang bo
     * @date 17-12-26
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void deleteNormalDev(RoutingContext routingContext) {
        logger.info("==LockHandler=deleteNormalDev==params->" + routingContext.getBodyAsString());
        //验证参数的合法性
        VerifyParamsUtil.verifyParams(routingContext, new JsonObject().put("dev_username", DataType.STRING)
                .put("adminid", DataType.STRING).put("devname", DataType.STRING), asyncResult -> {
            if (asyncResult.failed()) {
                routingContext.fail(401);
            } else {
                eventBus.send(AdminlockAddr.class.getName() + DELETE_NORMAL_DEV, asyncResult.result(), SendOptions.getInstance()
                        , (AsyncResult<Message<JsonObject>> rs) -> {
                            if (rs.failed()) {
                                routingContext.fail(501);
                            } else {
                                routingContext.response().end(JsonObject.mapFrom(new Result<String>()).toString());
                            }
                        });
            }
        });
    }


    /**
     * @Description 管理员为设备添加普通用户
     * @author zhang bo
     * @date 17-12-26
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void createNormalDev(RoutingContext routingContext) {
        logger.info("==LockHandler=createNormalDev==params->" + routingContext.getBodyAsString());
        //验证参数的合法性
        VerifyParamsUtil.verifyParams(routingContext, new JsonObject()
                .put("admin_id", DataType.STRING).put("device_username", DataType.STRING)
                .put("devicemac", DataType.STRING).put("devname", DataType.STRING)
                .put("end_time", DataType.STRING).put("lockNickName", DataType.STRING)
                .put("open_purview", DataType.STRING).put("start_time", DataType.STRING)
                .put("items", DataType.JSONARRAY), asyncResult -> {
            if (asyncResult.failed()) {
                routingContext.fail(401);
            } else {
                Result<JsonObject> result = new Result<>();
                eventBus.send(AdminlockAddr.class.getName() + CREATE_NORMAL_DEV, asyncResult.result(), SendOptions.getInstance()
                        , (AsyncResult<Message<JsonObject>> rs) -> {
                            if (rs.failed()) {
                                routingContext.fail(501);
                            } else {
                                if (Objects.nonNull(rs.result().body())) {
                                    result.setData(null);
                                    routingContext.response().end(JsonObject.mapFrom(result).toString());
                                } else {
                                    result.setErrorMessage(Integer.parseInt(rs.result().headers().get("code"))
                                            , rs.result().headers().get("msg"));
                                    routingContext.response().end(JsonObject.mapFrom(result).toString());
                                }
                            }
                        });
            }
        });
    }


    /**
     * @Description 获取开锁记录
     * @author zhang bo
     * @date 17-12-26
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void downloadOpenLocklist(RoutingContext routingContext) {
        logger.info("==LockHandler=downloadOpenLocklist==params->" + routingContext.getBodyAsString());
        //验证参数的合法性
        VerifyParamsUtil.verifyParams(routingContext, new JsonObject().put("pagenum", DataType.STRING)
                .put("device_name", DataType.STRING).put("user_id", DataType.STRING), asyncResult -> {
            if (asyncResult.failed()) {
                routingContext.fail(401);
            } else {
                eventBus.send(AdminlockAddr.class.getName() + GET_OPEN_LOCK_RECORD, asyncResult.result(), SendOptions.getInstance()
                        , (AsyncResult<Message<JsonArray>> rs) -> {
                            if (rs.failed()) {
                                routingContext.fail(501);
                            } else {
                                routingContext.response().end(JsonObject.mapFrom(new Result<JsonArray>().setData(rs.result().body())).toString());
                            }
                        });
            }
        });
    }

    /**
     * @Description 管理员修改普通用户权限
     * @author zhang bo
     * @date 17-12-26
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void updateNormalDevlock(RoutingContext routingContext) {
        logger.info("==LockHandler=updateNormalDevlock==params->" + routingContext.getBodyAsString());
        //验证参数的合法性
        VerifyParamsUtil.verifyParams(routingContext, new JsonObject().put("admin_id", DataType.STRING)
                .put("dateend", DataType.STRING).put("datestart", DataType.STRING)
                .put("dev_username", DataType.STRING).put("devname", DataType.STRING)
                .put("items", DataType.JSONARRAY).put("open_purview", DataType.STRING), asyncResult -> {
            if (asyncResult.failed()) {
                routingContext.fail(401);
            } else {
                eventBus.send(AdminlockAddr.class.getName() + UPDATE_USER_PREMISSON, asyncResult.result(), SendOptions.getInstance()
                        , (AsyncResult<Message<JsonObject>> rs) -> {
                            if (rs.failed()) {
                                routingContext.fail(501);
                            } else {
                                Result<JsonObject> result = new Result<>();
                                if (Objects.nonNull(rs.result().body())) {
                                    result.setData(null);
                                    routingContext.response().end(JsonObject.mapFrom(result).toString());
                                } else {
                                    result.setErrorMessage(ErrorType.UPDATE_USER_PREMISSION_FAIL.getKey()
                                            , ErrorType.UPDATE_USER_PREMISSION_FAIL.getValue());
                                    routingContext.response().end(JsonObject.mapFrom(result).toString());
                                }
                            }
                        });
            }
        });
    }


    /**
     * @Description 用户申请开锁
     * @author zhang bo
     * @date 17-12-26
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void adminOpenLock(RoutingContext routingContext) {
        logger.info("==LockHandler=adminOpenLock==params->" + routingContext.getBodyAsString());
        //验证参数的合法性
        VerifyParamsUtil.verifyParams(routingContext, new JsonObject().put("devname", DataType.STRING)
                .put("is_admin", DataType.STRING).put("open_type", DataType.STRING)
                .put("user_id", DataType.STRING), asyncResult -> {
            if (asyncResult.failed()) {
                routingContext.fail(401);
            } else {
                eventBus.send(AdminlockAddr.class.getName() + REQUEST_USER_OPEN_LOCK, asyncResult.result(), SendOptions.getInstance()
                        , (AsyncResult<Message<JsonObject>> rs) -> {
                            if (rs.failed()) {
                                routingContext.fail(501);
                            } else {
                                Result<JsonObject> result = new Result<>();
                                if (Objects.nonNull(rs.result().body())) {
                                    result.setData(null);
                                    routingContext.response().end(JsonObject.mapFrom(result).toString());
                                } else {
                                    result.setErrorMessage(Integer.parseInt(rs.result().headers().get("code"))
                                            , rs.result().headers().get("msg"));
                                    routingContext.response().end(JsonObject.mapFrom(result).toString());
                                }
                            }
                        });
            }
        });
    }

    /**
     * @Description 获取设备列表
     * @author zhang bo
     * @date 17-12-26
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void getAdminDevlist(RoutingContext routingContext) {
        logger.info("==LockHandler=getAdminDevlist==params->" + routingContext.getBodyAsString());
        //验证参数的合法性
        VerifyParamsUtil.verifyParams(routingContext, new JsonObject()
                .put("user_id", DataType.STRING), asyncResult -> {
            if (asyncResult.failed()) {
                routingContext.fail(401);
            } else {
                eventBus.send(AdminlockAddr.class.getName() + GET_DEV_LIST, asyncResult.result(), SendOptions.getInstance()
                        , (AsyncResult<Message<JsonArray>> rs) -> {
                            if (rs.failed()) {
                                routingContext.fail(501);
                            } else {
                                routingContext.response().end(JsonObject.mapFrom(new Result<JsonArray>().setData(
                                        rs.result().body())).toString().replaceAll("lockName", "device_name")
                                        .replaceAll("lockNickName", "device_nickname")
                                        .replaceAll("macLock", "devmac"));
                            }
                        });
            }
        });
    }


    /**
     * @Description 開鎖權權
     * @author zhang bo
     * @date 17-12-26
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void openLockAuth(RoutingContext routingContext) {
        logger.info("==LockHandler=getAdminDevlist==params->" + routingContext.getBodyAsString());
        //验证参数的合法性
        VerifyParamsUtil.verifyParams(routingContext, new JsonObject().put("devname", DataType.STRING)
                .put("is_admin", DataType.STRING).put("open_type", DataType.STRING)
                .put("user_id", DataType.STRING), asyncResult -> {
            if (asyncResult.failed()) {
                routingContext.fail(401);
            } else {
                eventBus.send(AdminlockAddr.class.getName() + LOCK_AUTH, asyncResult.result(), SendOptions.getInstance()
                        , (AsyncResult<Message<JsonArray>> rs) -> {
                            if (rs.failed()) {
                                routingContext.fail(501);
                            } else {
                                Result<JsonObject> result = new Result<>();
                                if (Objects.nonNull(rs.result().body())) {
                                    result.setData(null);
                                    routingContext.response().end(JsonObject.mapFrom(result).toString());
                                } else {
                                    result.setErrorMessage(Integer.parseInt(rs.result().headers().get("code"))
                                            , rs.result().headers().get("msg"));
                                    routingContext.response().end(JsonObject.mapFrom(result).toString());
                                }
                            }
                        });
            }
        });
    }

    /**
     * @Description 设备下的普通用户列表
     * @author zhang bo
     * @date 17-12-26
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void getNormalDevlist(RoutingContext routingContext) {
        logger.info("==LockHandler=getNormalDevlist==params->" + routingContext.getBodyAsString());
        //验证参数的合法性
        VerifyParamsUtil.verifyParams(routingContext, new JsonObject()
                .put("devname", DataType.STRING).put("user_id", DataType.STRING), asyncResult -> {
            if (asyncResult.failed()) {
                routingContext.fail(401);
            } else {
                eventBus.send(AdminlockAddr.class.getName() + GET_DEV_USER_LIST, asyncResult.result(), SendOptions.getInstance()
                        , (AsyncResult<Message<JsonArray>> rs) -> {
                            if (rs.failed()) {
                                routingContext.fail(501);
                            } else {
                                routingContext.response().end(JsonObject.mapFrom(new Result<JsonArray>().setData(rs.result().body())).toString());
                            }
                        });
            }
        });
    }


    /**
     * @Description 管理员修改锁的位置信息
     * @author zhang bo
     * @date 17-12-26
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void editAdminDev(RoutingContext routingContext) {
        logger.info("==LockHandler=editAdminDev==params->" + routingContext.getBodyAsString());
        //验证参数的合法性
        VerifyParamsUtil.verifyParams(routingContext, new JsonObject()
                .put("center_latitude", DataType.STRING).put("center_longitude", DataType.STRING)
                .put("circle_radius", DataType.STRING).put("devmac", DataType.STRING)
                .put("devname", DataType.STRING).put("edge_latitude", DataType.STRING)
                .put("edge_longitude", DataType.STRING).put("user_id", DataType.STRING), asyncResult -> {
            if (asyncResult.failed()) {
                routingContext.fail(401);
            } else {
                eventBus.send(AdminlockAddr.class.getName() + EDIT_ADMIN_DEV, asyncResult.result(), SendOptions.getInstance()
                        , (AsyncResult<Message<JsonObject>> rs) -> {
                            if (rs.failed()) {
                                routingContext.fail(501);
                            } else {
                                routingContext.response().end(JsonObject.mapFrom(new Result<String>()).toString());
                            }
                        });
            }
        });
    }

    /**
     * @Description 获取设备经纬度等信息
     * @author zhang bo
     * @date 17-12-26
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void getAdminDevlocklongtitude(RoutingContext routingContext) {
        logger.info("==LockHandler=getAdminDevlocklongtitude==params->" + routingContext.getBodyAsString());
        //验证参数的合法性
        VerifyParamsUtil.verifyParams(routingContext, new JsonObject()
                .put("devname", DataType.STRING).put("user_id", DataType.STRING), asyncResult -> {
            if (asyncResult.failed()) {
                routingContext.fail(401);
            } else {
                eventBus.send(AdminlockAddr.class.getName() + GET_DEV_LONGTITUDE, asyncResult.result(), SendOptions.getInstance()
                        , (AsyncResult<Message<JsonObject>> rs) -> {
                            if (rs.failed()) {
                                routingContext.fail(501);
                            } else {
                                routingContext.response().end(JsonObject.mapFrom(new Result<JsonObject>().setData(rs.result().body())).toString());
                            }
                        });
            }
        });
    }

    /**
     * @Description 修改设备是否开启自动解锁功能
     * @author zhang bo
     * @date 17-12-26
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void updateAdminDevAutolock(RoutingContext routingContext) {
        logger.info("==LockHandler=updateAdminDevAutolock==params->" + routingContext.getBodyAsString());
        //验证参数的合法性
        VerifyParamsUtil.verifyParams(routingContext, new JsonObject()
                .put("auto_lock", DataType.STRING).put("user_id", DataType.STRING)
                .put("devname", DataType.STRING), asyncResult -> {
            if (asyncResult.failed()) {
                routingContext.fail(401);
            } else {
                eventBus.send(AdminlockAddr.class.getName() + UPDATE_ADMIN_DEV_AUTO_LOCK, asyncResult.result(), SendOptions.getInstance()
                        , (AsyncResult<Message<JsonObject>> rs) -> {
                            if (rs.failed()) {
                                routingContext.fail(501);
                            } else {
                                routingContext.response().end(JsonObject.mapFrom(new Result<String>()).toString());
                            }
                        });
            }
        });
    }


    /**
     * @Description 修改设备昵称
     * @author zhang bo
     * @date 17-12-26
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void updateAdminlockNickName(RoutingContext routingContext) {
        logger.info("==LockHandler=updateAdminlockNickName==params->" + routingContext.getBodyAsString());
        //验证参数的合法性
        VerifyParamsUtil.verifyParams(routingContext, new JsonObject()
                .put("lockNickName", DataType.STRING).put("user_id", DataType.STRING)
                .put("devname", DataType.STRING), asyncResult -> {
            if (asyncResult.failed()) {
                routingContext.fail(401);
            } else {
                eventBus.send(AdminlockAddr.class.getName() + UPDATE_DEV_NICKNAME, asyncResult.result(), SendOptions.getInstance()
                        , (AsyncResult<Message<JsonObject>> rs) -> {
                            if (rs.failed()) {
                                routingContext.fail(501);
                            } else {
                                routingContext.response().end(JsonObject.mapFrom(new Result<String>()).toString());
                            }
                        });
            }
        });
    }

    /**
     * @Description 检测是否被绑定
     * @author zhang bo
     * @date 17-12-26
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void checkAdmindev(RoutingContext routingContext) {
        logger.info("==LockHandler=checkAdmindev==params->" + routingContext.getBodyAsString());
        //验证参数的合法性
        VerifyParamsUtil.verifyParams(routingContext, new JsonObject()
                .put("user_id", DataType.STRING).put("devname", DataType.STRING), asyncResult -> {
            if (asyncResult.failed()) {
                routingContext.fail(401);
            } else {
                eventBus.send(AdminlockAddr.class.getName() + CHECK_DEV, asyncResult.result(), SendOptions.getInstance()
                        , (AsyncResult<Message<JsonObject>> rs) -> {
                            if (rs.failed()) {
                                routingContext.fail(501);
                            } else {
                                routingContext.response().end(JsonObject.mapFrom(new Result<JsonObject>().setErrorMessage(Integer.parseInt(rs.result().headers().get("code"))
                                        , rs.result().headers().get("msg"))).toString());
                            }
                        });
            }
        });
    }


    /**
     * @Description 上传开门记录
     * @author zhang bo
     * @date 17-12-26
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void uploadOpenLockList(RoutingContext routingContext) {
        logger.info("==LockHandler=uploadOpenLockList==params->" + routingContext.getBodyAsString());
        //验证参数的合法性
        VerifyParamsUtil.verifyParams(routingContext, new JsonObject()
                .put("device_name", DataType.STRING).put("device_nickname", DataType.STRING)
                .put("openLockList", DataType.JSONARRAY).put("user_id", DataType.STRING), asyncResult -> {
            if (asyncResult.failed()) {
                routingContext.fail(401);
            } else {
                eventBus.send(AdminlockAddr.class.getName() + UPLOAD_OPEN_LOCK_RECORD, asyncResult.result(), SendOptions.getInstance()
                        , (AsyncResult<Message<JsonObject>> rs) -> {
                            if (rs.failed()) {
                                routingContext.fail(501);
                            } else {
                                routingContext.response().end(JsonObject.mapFrom(new Result<String>()).toString());
                            }
                        });
            }
        });
    }

    /**
     * @Description 修改锁的信息
     * @author zhang bo
     * @date 18-5-22
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    @Deprecated
    public void updateLockInfo(RoutingContext routingContext) {
        //验证参数的合法性
        VerifyParamsUtil.verifyParams(routingContext, new JsonObject().put("lockNickName", DataType.STRING)
                .put("devname", DataType.STRING).put("uid", DataType.STRING), asyncResult -> {
            if (asyncResult.failed()) {
                routingContext.fail(401);
            } else {
                eventBus.send(AdminlockAddr.class.getName() + UPDATE_LOCK_INFO, asyncResult.result()
                        , SendOptions.getInstance(), rs -> {
                            if (rs.failed()) {
                                routingContext.fail(501);
                            } else {
                                if (Objects.nonNull(rs.result().body())) {
                                    routingContext.response().end(JsonObject.mapFrom(new Result<String>()
                                            .setErrorMessage(ErrorType.OPERATION_FAIL.getKey(), ErrorType.OPERATION_FAIL.getValue())).toString());
                                } else {
                                    routingContext.response().end(JsonObject.mapFrom(new Result<String>()).toString());
                                }
                            }
                        });
            }
        });
    }


    /**
     * @Description 上传无需鉴权的开门记录
     * @author zhang bo
     * @date 18-7-11
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void openLockNoAuthRecord(RoutingContext routingContext) {
        //验证参数的合法性
        VerifyParamsUtil.verifyParams(routingContext, new JsonObject().put("devname", DataType.STRING)
                .put("open_type", DataType.STRING).put("uid", DataType.STRING)
                .put("open_purview",DataType.STRING), asyncResult -> {
            if (asyncResult.failed()) {
                routingContext.fail(401);
            } else {
                eventBus.send(AdminlockAddr.class.getName() + OPEN_LOCK_NO_AUTH_SUCCESS, asyncResult.result()
                        , SendOptions.getInstance(), rs -> {
                            if (rs.failed()) {
                                routingContext.fail(501);
                            } else {
                                if (Objects.nonNull(rs.result().body())) {
                                    routingContext.response().end(JsonObject.mapFrom(new Result<String>()).toString());
                                } else {
                                    routingContext.response().end(JsonObject.mapFrom(new Result<String>()
                                            .setErrorMessage(ErrorType.OPERATION_FAIL.getKey(), ErrorType.OPERATION_FAIL.getValue())).toString());
                                }
                            }
                        });
            }
        });
    }

    /**
     * @Description 查詢开门記錄
     * @author zhang bo
     * @date 18-7-13
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void selectOpenLockRecord(RoutingContext routingContext) {
        //验证参数的合法性
        VerifyParamsUtil.verifyParams(routingContext, new JsonObject().put("uid", DataType.STRING)
                .put("content", DataType.STRING).put("start_time", DataType.STRING)
                .put("end_time", DataType.STRING).put("devname", DataType.STRING)
                .put("page", DataType.INTEGER).put("pageNum", DataType.INTEGER), asyncResult -> {
            if (asyncResult.failed()) {
                routingContext.fail(401);
            } else {
                eventBus.send(AdminlockAddr.class.getName() + SELECT_OPNELOCK_RECORD, asyncResult.result()
                        , SendOptions.getInstance(), (AsyncResult<Message<JsonArray>> rs) -> {
                            if (rs.failed()) {
                                routingContext.fail(501);
                            } else {
                                if (Objects.nonNull(rs.result().body())) {
                                    routingContext.response().end(JsonObject.mapFrom(new Result<JsonArray>()
                                            .setData(rs.result().body())).toString());
                                } else {
                                    routingContext.response().end(JsonObject.mapFrom(new Result<String>()
                                            .setErrorMessage(ErrorType.OPERATION_FAIL.getKey(), ErrorType.OPERATION_FAIL.getValue())).toString());
                                }
                            }
                        });
            }
        });
    }


    /**
     * @Description 修改锁编号信息
     * @author zhang bo
     * @date 18-7-13
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void updateLockNumberInfo(RoutingContext routingContext) {
        //验证参数的合法性
        VerifyParamsUtil.verifyParams(routingContext, new JsonObject().put("uid", DataType.STRING)
                .put("num", DataType.STRING).put("devname", DataType.STRING)
                .put("numNickname", DataType.STRING), asyncResult -> {
            if (asyncResult.failed()) {
                routingContext.fail(401);
            } else {
                eventBus.send(AdminlockAddr.class.getName() + UPDATE_LOCK_NUM_INFO, asyncResult.result()
                        , SendOptions.getInstance(), rs -> {
                            if (rs.failed()) {
                                routingContext.fail(501);
                            } else {
                                if (Objects.nonNull(rs.result().body())) {
                                    routingContext.response().end(JsonObject.mapFrom(new Result<String>()).toString());
                                } else {
                                    routingContext.response().end(JsonObject.mapFrom(new Result<String>()
                                            .setErrorMessage(ErrorType.OPERATION_FAIL.getKey(), ErrorType.OPERATION_FAIL.getValue())).toString());
                                }
                            }
                        });
            }
        });
    }


    /**
     * @Description 獲取lock編號信息
     * @author zhang bo
     * @date 18-7-13
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void getLockNumberInfo(RoutingContext routingContext) {
        //验证参数的合法性
        VerifyParamsUtil.verifyParams(routingContext, new JsonObject().put("uid", DataType.STRING)
                .put("devname", DataType.STRING), asyncResult -> {
            if (asyncResult.failed()) {
                routingContext.fail(401);
            } else {
                eventBus.send(AdminlockAddr.class.getName() + GET_LOCK_NUM_INFO, asyncResult.result()
                        , SendOptions.getInstance(), (AsyncResult<Message<JsonArray>> rs) -> {
                            if (rs.failed()) {
                                routingContext.fail(501);
                            } else {
                                if (Objects.nonNull(rs.result().body())) {
                                    routingContext.response().end(JsonObject.mapFrom(new Result<JsonArray>()
                                            .setData(rs.result().body())).toString());
                                } else {
                                    routingContext.response().end(JsonObject.mapFrom(new Result<String>()
                                            .setErrorMessage(ErrorType.OPERATION_FAIL.getKey(), ErrorType.OPERATION_FAIL.getValue())).toString());
                                }
                            }
                        });
            }
        });
    }
}
