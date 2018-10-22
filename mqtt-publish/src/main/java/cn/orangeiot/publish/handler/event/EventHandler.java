package cn.orangeiot.publish.handler.event;

import cn.orangeiot.common.options.SendOptions;
import cn.orangeiot.common.utils.DataType;
import cn.orangeiot.common.verify.VerifyParamsUtil;
import cn.orangeiot.publish.handler.event.device.DeviceHandler;
import cn.orangeiot.publish.service.UserService;
import cn.orangeiot.publish.service.impl.UserServiceImpl;
import cn.orangeiot.reg.event.EventAddr;
import cn.orangeiot.reg.message.MessageAddr;
import cn.orangeiot.reg.user.UserAddr;
import io.vertx.core.*;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.w3c.dom.UserDataHandler;
import scala.util.parsing.json.JSONArray;
import sun.security.x509.CertAttrSet;

import java.util.Objects;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-01-16
 */
public class EventHandler implements EventAddr, MessageAddr, UserAddr {

    private static Logger logger = LogManager.getLogger(EventHandler.class);

    private Vertx vertx;

    private JsonObject jsonObject;

    private DeviceHandler deviceHandler;

    private UserService userService;

    public EventHandler(Vertx vertx, JsonObject jsonObject) {
        this.vertx = vertx;
        this.jsonObject = jsonObject;
        deviceHandler = new DeviceHandler(vertx, jsonObject);
        userService = new UserServiceImpl(vertx, jsonObject);
    }

    /**
     * @Description event事件處理
     * @author zhang bo
     * @date 18-1-16
     * @version 1.0
     */
    public void onEventMessage(Message<JsonObject> message, Handler<AsyncResult<JsonObject>> handler) {
        logger.debug("==EventHandler=onEventMessage params -> " + message.body());
        VerifyParamsUtil.verifyParams(message.body(), new JsonObject().put("msgtype", DataType.STRING)
                , (AsyncResult<JsonObject> rs) -> {
                    if (rs.failed()) {
//                        message.reply(new JsonObject().put("code", 401));//参数校验失败
                        handler.handle(Future.failedFuture(""));
                    } else {
                        boolean flag = false;
                        try {
                            flag = redirectProcess(rs.result(), message.headers());
                        } catch (Exception e) {
                            logger.error(e.getMessage(), e);
                        }
                        if (flag) {
                            //是否存在用户
                            if (!Objects.nonNull(message.body().getValue("userId")) || (Objects.nonNull(message.body().getValue("userId")) &&
                                    message.body().getString("userId").equals("EMPTY"))) {
                                vertx.eventBus().send(EventAddr.class.getName() + GET_GATEWAY_ADMIN_ALL, rs.result(), SendOptions.getInstance()
                                        , (AsyncResult<Message<JsonArray>> as) -> {
                                            if (as.failed()) {
                                                handler.handle(Future.failedFuture(as.cause().getMessage()));
                                            } else {
                                                if (Objects.nonNull(as.result()) && as.result().body().size() > 0) {
                                                    handler.handle(Future.failedFuture("========gateway user size " + as.result().body().size()));
                                                    as.result().body().stream().forEach(e -> {
                                                        JsonObject jsonObject = (JsonObject) e;
                                                        vertx.eventBus().send(MessageAddr.class.getName() + SEND_ADMIN_MSG, message.body(),
                                                                SendOptions.getInstance().addHeader("qos", message.headers().get("qos"))
                                                                        .addHeader("uid", jsonObject.getString("uid")).addHeader("redict", "1")
                                                                        .addHeader("messageId", message.headers().get("messageId")));
                                                    });
                                                } else {
                                                    handler.handle(Future.failedFuture("========gateway no have admin"));
                                                }
                                            }
                                        });
                            } else {
                                handler.handle(Future.failedFuture("========gateway send a user"));
                                vertx.eventBus().send(MessageAddr.class.getName() + SEND_ADMIN_MSG, message.body(),
                                        SendOptions.getInstance().addHeader("qos", message.headers().get("qos"))
                                                .addHeader("uid", message.body().getString("userId"))
                                                .addHeader("messageId", message.headers().get("messageId")));
                                //通知管理员
                                vertx.eventBus().send(UserAddr.class.getName() + GET_GW_ADMIN, new JsonObject().put("gwId", ""), (AsyncResult<Message<JsonObject>> ars) -> {
                                    if (ars.failed()) {
                                        logger.error(ars.cause().getMessage(), ars);
                                    } else {
                                        if (Objects.nonNull(ars.result().body()) && !ars.result().body().getString("adminuid").equals(message.body().getString("userId")))
                                            vertx.eventBus().send(MessageAddr.class.getName() + SEND_ADMIN_MSG, message.body(),
                                                    SendOptions.getInstance().addHeader("qos", message.headers().get("qos"))
                                                            .addHeader("uid", ars.result().body().getString("adminuid")).addHeader("redict", "1")
                                                            .addHeader("messageId", message.headers().get("messageId")));
                                    }
                                });
                            }
                        } else {
                            handler.handle(Future.failedFuture("========event no send"));
                        }
                    }
                });
    }


    /**
     * @Description 中轉處理
     * @author zhang bo
     * @date 18-3-22
     * @version 1.0
     */
    public boolean redirectProcess(JsonObject message, MultiMap headers) throws Exception {
        logger.info("==EventHandler=redirectProcess params -> " + message.toString());
        if (Objects.nonNull(message.getValue("func"))) {
            switch (message.getString("func")) {
                case "gwevent":
                    if (Objects.nonNull(message.getJsonObject("eventparams").getValue("event_str"))) {
                        switch (message.getJsonObject("eventparams").getString("event_str")) {//判断时间
                            case "online"://設備上報
                                deviceHandler.deviceOnline(message);
                                return true;
                            case "offline"://設備下線
                                deviceHandler.deviceOffline(message);
                                return true;
                            case "delete"://設備删除
                                deviceHandler.devicedelete(message);
                                return true;
                            case "getDevList"://設備列表
                                deviceHandler.getDeviceList(message, headers);
                                return false;
                            default:
                                logger.warn("==EventHandler=redirectProcess not case function -> " + message.getString(""));
                                return false;
                        }
                    } else if (Objects.nonNull(message.getJsonObject("eventparams").getValue("devecode"))
                            && message.getJsonObject("eventparams").getInteger("devecode") == 2
                            && Objects.nonNull(message.getJsonObject("eventparams").getValue("devetype"))
                            && message.getJsonObject("eventparams").getString("devetype").equals("lockop")) {//开门
                        deviceHandler.openLock(message);
                        return true;
                    } else {
                        return true;
                    }
                case "gatewayReset"://网关重置
                    deviceHandler.resetDevice(message, headers, jsonObject);
                    return true;
                case "selectGWAdmin"://获取网关管理员
                    userService.selectGWAdmin(message);
                    return false;
                default:
                    logger.warn("==EventHandler=redirectProcess not case func -> " + message.getString(""));
                    return true;
            }
        } else {
            logger.warn("==EventHandler=redirectProcess func is null -> " + message.getValue("func"));
            return false;
        }
    }

}
