package cn.orangeiot.publish.handler.event;

import cn.orangeiot.common.options.SendOptions;
import cn.orangeiot.common.utils.DataType;
import cn.orangeiot.common.verify.VerifyParamsUtil;
import cn.orangeiot.publish.handler.event.device.DeviceHandler;
import cn.orangeiot.reg.event.EventAddr;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import sun.security.x509.CertAttrSet;

import java.util.Objects;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-01-16
 */
public class EventHandler implements EventAddr {

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
        logger.info("==EventHandler=onEventMessage params -> " + message.body());
        VerifyParamsUtil.verifyParams(message.body(), new JsonObject().put("msgtype", DataType.STRING)
                , (AsyncResult<JsonObject> rs) -> {
                    if (rs.failed()) {
                        message.reply(new JsonObject().put("code", 401));//参数校验失败
                    } else {
                        try {
                            redirectProcess(rs.result());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        //是否存在用户时间
                        if (!Objects.nonNull(message.body().getString("userId"))) {
                            vertx.eventBus().send(EventAddr.class.getName() + GET_GATEWAY_ADMIN_UID, rs.result(), SendOptions.getInstance()
                                    , (AsyncResult<Message<JsonObject>> as) -> {
                                        if (as.failed()) {
                                            as.cause().printStackTrace();
                                        } else {
                                            if (Objects.nonNull(as.result().body()))
                                                handler.handle(Future.succeededFuture(message.body().put("topicName", jsonObject.getString("reply_message").replace("clientId",
                                                        as.result().body().getString("uid")))));
                                            else
                                                handler.handle(Future.failedFuture("========gateway no have admin"));
                                        }
                                    });
                        } else {
                            handler.handle(Future.succeededFuture(message.body().put("topicName", jsonObject.getString("reply_message").replace("clientId",
                                    message.body().getString("userId")))));
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
    public void redirectProcess(JsonObject message) throws Exception {
        logger.info("==EventHandler=redirectProcess params -> " + message.toString());
        if (Objects.nonNull(message.getValue("eventcode"))) {
            if (message.getInteger("eventcode") == 1) {//设备管理消息
                switch (message.getJsonObject("eventparams").getString("event_str")) {//判断时间
                    case "online"://設備上報
                        deviceHandler.deviceOnline(message);
                        break;
                    case "offline"://設備下線
                        deviceHandler.deviceOffline(message);
                        break;
                    default:
                        logger.warn("==EventHandler=redirectProcess not case function -> " + message.getString(""));
                        break;
                }
            } else {//设备自定义消息
                logger.info("==EventHandler=redirectProcess eventcode function -> " + message.getValue("eventcode"));
            }
        } else {
            logger.info("==EventHandler=redirectProcess eventcode is null -> " + message.getValue("eventcode"));
        }
    }

}
