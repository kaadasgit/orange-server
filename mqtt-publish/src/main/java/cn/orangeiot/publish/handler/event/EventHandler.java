package cn.orangeiot.publish.handler.event;

import cn.orangeiot.common.options.SendOptions;
import cn.orangeiot.common.utils.DataType;
import cn.orangeiot.common.verify.VerifyParamsUtil;
import cn.orangeiot.publish.handler.event.device.DeviceHandler;
import cn.orangeiot.reg.event.EventAddr;
import cn.orangeiot.reg.message.MessageAddr;
import cn.orangeiot.reg.user.UserAddr;
import io.vertx.core.*;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
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

    public EventHandler(Vertx vertx, JsonObject jsonObject) {
        this.vertx = vertx;
        this.jsonObject = jsonObject;
        deviceHandler = new DeviceHandler(vertx, jsonObject);
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
                        message.reply(new JsonObject().put("code", 401));//参数校验失败
                    } else {
                        try {
                            redirectProcess(rs.result(), message.headers());
                        } catch (Exception e) {
                            logger.error(e.getMessage(), e);
                        }
                        //是否存在用户
                        if (!Objects.nonNull(message.body().getString("userId"))) {
                            vertx.eventBus().send(EventAddr.class.getName() + GET_GATEWAY_ADMIN_ALL, rs.result(), SendOptions.getInstance()
                                    , (AsyncResult<Message<JsonArray>> as) -> {
                                        if (as.failed()) {
                                            logger.error(as.cause().getMessage(), as);
                                        } else {
                                            if (Objects.nonNull(as.result()) && as.result().body().size() > 0) {
                                                handler.handle(Future.failedFuture("========gateway user size " + as.result().body().size()));
                                                as.result().body().stream().forEach(e -> {
                                                    JsonObject jsonObject = (JsonObject) e;
                                                    vertx.eventBus().send(MessageAddr.class.getName() + SEND_ADMIN_MSG, message.body(),
                                                            SendOptions.getInstance().addHeader("qos", message.headers().get("qos"))
                                                                    .addHeader("uid", jsonObject.getString("uid")).addHeader("redict", "1"));
                                                });
                                            } else {
                                                handler.handle(Future.failedFuture("========gateway no have admin"));
                                            }
                                        }
                                    });
                        } else {
                            handler.handle(Future.succeededFuture(message.body().put("topicName", jsonObject.getString("reply_message").replace("clientId",
                                    message.body().getString("userId")))));
                            //通知管理员
                            vertx.eventBus().send(UserAddr.class.getName() + GET_GW_ADMIN, new JsonObject().put("gwId", ""), (AsyncResult<Message<JsonObject>> ars) -> {
                                if (ars.failed()) {
                                    logger.error(ars.cause().getMessage(), ars);
                                } else {
                                    if (Objects.nonNull(ars.result().body()) && !ars.result().body().getString("adminuid").equals(message.body().getString("userId")))
                                        vertx.eventBus().send(MessageAddr.class.getName() + SEND_ADMIN_MSG, message.body(),
                                                SendOptions.getInstance().addHeader("qos", message.headers().get("qos"))
                                                        .addHeader("uid", ars.result().body().getString("adminuid")).addHeader("redict", "1"));
                                }
                            });
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
    public void redirectProcess(JsonObject message, MultiMap headers) throws Exception {
        logger.info("==EventHandler=redirectProcess params -> " + message.toString());
        if (Objects.nonNull(message.getValue("func"))) {
            switch (message.getString("func")) {
                case "gwevent":
                    if (Objects.nonNull(message.getJsonObject("eventparams").getValue("event_str"))) {
                        switch (message.getJsonObject("eventparams").getString("event_str")) {//判断时间
                            case "online"://設備上報
                                deviceHandler.deviceOnline(message);
                                break;
                            case "offline"://設備下線
                                deviceHandler.deviceOffline(message);
                                break;
                            case "delete"://設備删除
                                deviceHandler.devicedelete(message);
                                break;
                            case "getDevList"://設備列表
                                deviceHandler.getDeviceList(message);
                                break;
                            default:
                                logger.warn("==EventHandler=redirectProcess not case function -> " + message.getString(""));
                                break;
                        }
                    } else if (Objects.nonNull(message.getJsonObject("eventparams").getValue("devecode"))
                            && message.getJsonObject("eventparams").getInteger("devecode") == 2) {//开门
                        deviceHandler.openLock(message);
                    }
                    break;
                case "gatewayReset":
                    deviceHandler.resetDevice(message, headers, jsonObject);
                    break;
                default:
                    logger.warn("==EventHandler=redirectProcess not case func -> " + message.getString(""));
                    break;
            }
        } else {
            logger.warn("==EventHandler=redirectProcess func is null -> " + message.getValue("func"));
        }
    }

}
