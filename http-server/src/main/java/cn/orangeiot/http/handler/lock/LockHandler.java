package cn.orangeiot.http.handler.lock;

import cn.orangeiot.common.genera.ErrorType;
import cn.orangeiot.common.genera.Result;
import cn.orangeiot.common.options.SendOptions;
import cn.orangeiot.http.verify.VerifyParamsUtil;
import cn.orangeiot.reg.adminlock.AdminlockAddr;
import io.vertx.core.AsyncResult;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2017-12-26
 */
public class LockHandler implements AdminlockAddr {

    private static Logger logger = LoggerFactory.getLogger(LockHandler.class);

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
        VerifyParamsUtil.verifyParams(routingContext, new JsonObject().put("devmac", String.class.getName())
                .put("devname", String.class.getName()).put("user_id", String.class.getName()), asyncResult -> {
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
                                    result.setErrorMessage(ErrorType.DEV_REGED.getKey(), ErrorType.DEV_REGED.getValue());
                                    routingContext.response().end(JsonObject.mapFrom(result).toString());
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
        VerifyParamsUtil.verifyParams(routingContext, new JsonObject().put("adminid", String.class.getName())
                .put("devname", String.class.getName()), asyncResult -> {
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
        VerifyParamsUtil.verifyParams(routingContext, new JsonObject().put("devname", String.class.getName())
                .put("adminid", String.class.getName()), asyncResult -> {
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
        VerifyParamsUtil.verifyParams(routingContext, new JsonObject().put("dev_username", String.class.getName())
                .put("adminid", String.class.getName()).put("devname", String.class.getName()), asyncResult -> {
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
                .put("admin_id", String.class.getName()).put("device_username", String.class.getName())
                .put("devicemac", String.class.getName()).put("devname", String.class.getName())
                .put("end_time", String.class.getName()).put("lockNickName", String.class.getName())
                .put("open_purview", String.class.getName()).put("start_time", String.class.getName())
                .put("items", JsonArray.class.getName()), asyncResult -> {
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
        VerifyParamsUtil.verifyParams(routingContext, new JsonObject().put("pagenum", String.class.getName())
                .put("device_name", String.class.getName()).put("user_id", String.class.getName()), asyncResult -> {
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
        VerifyParamsUtil.verifyParams(routingContext, new JsonObject().put("admin_id", String.class.getName())
                .put("dateend", String.class.getName()).put("datestart", String.class.getName())
                .put("dev_username", String.class.getName()).put("devname", String.class.getName())
                .put("items", JsonArray.class.getName()).put("open_purview", String.class.getName()), asyncResult -> {
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
        VerifyParamsUtil.verifyParams(routingContext, new JsonObject().put("devname", String.class.getName())
                .put("is_admin", String.class.getName()).put("open_type", String.class.getName())
                .put("user_id", String.class.getName()), asyncResult -> {
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
                .put("user_id", String.class.getName()), asyncResult -> {
            if (asyncResult.failed()) {
                routingContext.fail(401);
            } else {
                eventBus.send(AdminlockAddr.class.getName() + GET_DEV_LIST, asyncResult.result(), SendOptions.getInstance()
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
                .put("devname", String.class.getName()).put("user_id", String.class.getName()), asyncResult -> {
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
                .put("center_latitude", String.class.getName()).put("center_longitude", String.class.getName())
                .put("circle_radius", String.class.getName()).put("devmac", String.class.getName())
                .put("devname", String.class.getName()).put("edge_latitude", String.class.getName())
                .put("edge_longitude", String.class.getName()).put("user_id", String.class.getName()), asyncResult -> {
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
                .put("devname", String.class.getName()).put("user_id", String.class.getName()), asyncResult -> {
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
                .put("auto_lock", String.class.getName()).put("user_id", String.class.getName())
                .put("devname", String.class.getName()), asyncResult -> {
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
                .put("lockNickName", String.class.getName()).put("user_id", String.class.getName())
                .put("devname", String.class.getName()), asyncResult -> {
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
                .put("user_id", String.class.getName()).put("devname", String.class.getName()), asyncResult -> {
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
                .put("device_name", String.class.getName()).put("device_nickname", String.class.getName())
                .put("openLockList", JsonArray.class.getName()).put("user_id", String.class.getName()), asyncResult -> {
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
}
