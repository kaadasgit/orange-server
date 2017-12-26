package cn.orangeiot.http.handler.lock;

import cn.orangeiot.common.genera.ErrorType;
import cn.orangeiot.common.genera.Result;
import cn.orangeiot.http.verify.VerifyParamsUtil;
import cn.orangeiot.reg.adminlock.AdminlockAddr;
import io.vertx.core.AsyncResult;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;

import java.util.Objects;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2017-12-26
 */
public class LockHandler implements AdminlockAddr{

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
    public void createAdminDev(RoutingContext routingContext){
        logger.info("==LockHandler=createAdminDev==params->" + routingContext.getBodyAsString());
        //验证参数的合法性
        VerifyParamsUtil.verifyParams(routingContext, new JsonObject().put("devmac", String.class.getName())
                .put("devname", String.class.getName()).put("nickname", String.class.getName())
                        .put("user_id", String.class.getName()), asyncResult -> {
            if (asyncResult.failed()) {
                routingContext.fail(401);
            } else {
                Result<JsonObject> result = new Result<>();
                eventBus.send(AdminlockAddr.class.getName()+CREATE_ADMIN_DEV, asyncResult.result(), (AsyncResult<Message<JsonObject>> rs) -> {
                    if (rs.failed()) {
                        routingContext.fail(501);
                    } else {
                        if (Objects.nonNull(rs.result().body())) {
                            result.setData(rs.result().body());
                            routingContext.response().end(JsonObject.mapFrom(result).toString());
                        } else {
                            result.setErrorMessage(ErrorType.RESULT_LOGIN_FIAL.getKey(), ErrorType.RESULT_LOGIN_FIAL.getValue());
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
    public void deletevendorDev(RoutingContext routingContext){

    }


    /**
     * @Description 用户主动删除设备
     * @author zhang bo
     * @date 17-12-26
     * @version 1.0
     */
    public void deleteAdminDev(RoutingContext routingContext){

    }


    /**
     * @Description 管理员删除用户
     * @author zhang bo
     * @date 17-12-26
     * @version 1.0
     */
    public void deleteNormalDev(RoutingContext routingContext){

    }


    /**
     * @Description 管理员为设备添加普通用户
     * @author zhang bo
     * @date 17-12-26
     * @version 1.0
     */
    public void createNormalDev(RoutingContext routingContext){

    }


    /**
     * @Description 获取开锁记录
     * @author zhang bo
     * @date 17-12-26
     * @version 1.0
     */
    public void downloadOpenLocklist(RoutingContext routingContext){

    }

    /**
     * @Description 管理员修改普通用户权限
     * @author zhang bo
     * @date 17-12-26
     * @version 1.0
     */
    public void updateNormalDevlock(RoutingContext routingContext){

    }


    /**
     * @Description 用户申请开锁
     * @author zhang bo
     * @date 17-12-26
     * @version 1.0
     */
    public void adminOpenLock(RoutingContext routingContext){

    }

    /**
     * @Description 获取设备列表
     * @author zhang bo
     * @date 17-12-26
     * @version 1.0
     */
    public void getAdminDevlist(RoutingContext routingContext){

    }

    /**
     * @Description 设备下的普通用户列表
     * @author zhang bo
     * @date 17-12-26
     * @version 1.0
     */
    public void getNormalDevlist(RoutingContext routingContext){

    }


    /**
     * @Description 管理员修改锁的位置信息
     * @author zhang bo
     * @date 17-12-26
     * @version 1.0
     */
    public void editAdminDev(RoutingContext routingContext){

    }

    /**
     * @Description 获取设备经纬度等信息
     * @author zhang bo
     * @date 17-12-26
     * @version 1.0
     */
    public void getAdminDevlocklongtitude(RoutingContext routingContext){

    }

    /**
     * @Description 修改设备是否开启自动解锁功能
     * @author zhang bo
     * @date 17-12-26
     * @version 1.0
     */
    public void updateAdminDevAutolock(RoutingContext routingContext){

    }


    /**
     * @Description 修改设备是否开启自动解锁功能
     * @author zhang bo
     * @date 17-12-26
     * @version 1.0
     */
    public void updateAdminlockNickName(RoutingContext routingContext){

    }
    /**
     * @Description 检测是否被绑定
     * @author zhang bo
     * @date 17-12-26
     * @version 1.0
     */
    public void checkAdmindev(RoutingContext routingContext){

    }


    /**
     * @Description 上传开门记录
     * @author zhang bo
     * @date 17-12-26
     * @version 1.0
     */
    public void uploadOpenLockList(RoutingContext routingContext){

    }
}
