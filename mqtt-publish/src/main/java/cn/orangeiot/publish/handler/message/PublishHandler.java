package cn.orangeiot.publish.handler.message;

import cn.orangeiot.common.utils.DataType;
import cn.orangeiot.common.verify.VerifyParamsUtil;
import cn.orangeiot.publish.handler.event.EventHandler;
import cn.orangeiot.reg.message.MessageAddr;
import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.util.Objects;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2017-12-11
 */
public class PublishHandler implements MessageAddr {

    private static Logger logger = LogManager.getLogger(PublishHandler.class);

    private JsonObject jsonObject;

    private FuncHandler funcHandler;

    private EventHandler eventHandler;

    private Vertx vertx;

    public PublishHandler(JsonObject jsonObject, FuncHandler funcHandler, EventHandler eventHandler, Vertx vertx) {
        this.jsonObject = jsonObject;
        this.funcHandler = funcHandler;
        this.eventHandler = eventHandler;
        this.vertx = vertx;
    }

    /**
     * @Description 消息处理
     * @author zhang bo
     * @date 17-12-11
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void onMessage(Message<JsonObject> message) {
        logger.debug("==PublishHandler=onMessage==params -> " + message.body().toString());
        VerifyParamsUtil.verifyParams(message.body(), new JsonObject().put("userId", DataType.STRING)
                .put("deviceId", DataType.STRING).put("gwId", DataType.STRING).put("topicName", DataType.STRING)
                .put("clientId", DataType.STRING), (AsyncResult<JsonObject> rs) -> {
            if (rs.failed()) {
                //app业务
                if (Objects.nonNull(message.body().getValue("topicName")) && message.body().getString("topicName").equals(jsonObject.getString("app_fuc_message"))) {
                    funcHandler.onMessage(message, (AsyncResult<JsonObject> returnData) -> {
                        if (returnData.failed()) {
                            message.reply(new JsonObject().put("code", 404).put("msg", returnData.cause().getMessage())
                                            .put("topicName", jsonObject.getString("reply_message").replace("clientId",
                                                    message.body().getString("uid"))),
                                    new DeliveryOptions().addHeader("messageId", message.headers().get("messageId"))
                                            .addHeader("qos", message.headers().get("qos")));
                        } else {
                            message.reply(returnData.result().put("topicName", jsonObject.getString("reply_message").replace("clientId",
                                    message.body().getString("uid"))),
                                    new DeliveryOptions().addHeader("messageId", message.headers().get("messageId"))
                                            .addHeader("qos", message.headers().get("qos")));
                        }
                    });
                } else if (Objects.nonNull(message.body().getValue("topicName")) && message.body().getString("topicName").indexOf("/event") >= 0) {//网关事件上报
                    eventHandler.onEventMessage(message, as -> {
                        if (as.failed()) {
                            logger.error(as.cause().getMessage());
                            message.reply(null);
                        } else {
                            jsonObject.getString("app_fuc_message");
                            message.reply(as.result(),
                                    new DeliveryOptions().addHeader("messageId", message.headers().get("messageId"))
                                            .addHeader("qos", message.headers().get("qos")));
                        }
                    });
                } else if (Objects.nonNull(message.body().getValue("topicName")) && message.body().getString("topicName").indexOf("/rpc/call") > 0) {
                    funcHandler.onMessage(message, (AsyncResult<JsonObject> returnData) -> {
                        if (returnData.failed()) {
                            returnData.cause().printStackTrace();
                        } else {
                            message.reply(null);
                            DeliveryOptions deliveryOptions = new DeliveryOptions().addHeader("uid"
                                    , "gw:" + returnData.result().getString("gwId"))
                                    .addHeader("topic", MessageAddr.SEND_GATEWAY_REPLAY.replace("gwId"
                                            , returnData.result().getString("gwId")))
                                    .addHeader("messageId", message.headers().get("messageId"))
                                    .addHeader("qos", message.headers().get("qos"));
                            returnData.result().remove("topicName");
                            returnData.result().remove("clientId");
                            vertx.eventBus().send(MessageAddr.class.getName() + SEND_UPGRADE_MSG, returnData.result()
                                    , deliveryOptions);
                        }
                    });
                } else {
                    message.reply(new JsonObject().put("code", 401)
                            , new DeliveryOptions().addHeader("messageId", message.headers().get("messageId"))
                                    .addHeader("qos", message.headers().get("qos")));//参数校验失败
                }
            } else {
                String flag = rs.result().getString("clientId").split(":")[0];
                funcHandler.onRpcMessage(message, as -> {
                    if (as.failed()) {
                        logger.error(as.cause().getMessage(),as);
                        message.reply(null);
                    } else {
                        if (Objects.nonNull(message.body().getValue("topicName")) && message.body().getString("topicName").indexOf("/event") >= 0) {
                            eventHandler.onEventMessage(message, eventRs -> {
                                if (eventRs.failed()) {
                                    logger.error(eventRs.cause().getMessage());
                                    message.reply(null);
                                } else {
                                    jsonObject.getString("app_fuc_message");
                                    message.reply(eventRs.result(),
                                            new DeliveryOptions().addHeader("messageId", message.headers().get("messageId"))
                                                    .addHeader("qos", message.headers().get("qos")));
                                }
                            });
                        } else if (flag.equals("app")) {//app 发送
                            message.reply(new JsonObject().put("flag", true).put("topicName", jsonObject.getString("repeat_message").replace("gwId",
                                    message.body().getString("gwId"))),
                                    new DeliveryOptions().addHeader("messageId", message.headers().get("messageId"))
                                            .addHeader("qos", message.headers().get("qos")));
                        } else {//gw 发送
                            //reply回复app
                            message.reply(new JsonObject().put("flag", true).put("topicName", jsonObject.getString("reply_message").replace("clientId",
                                    message.body().getString("userId"))),
                                    new DeliveryOptions().addHeader("messageId", message.headers().get("messageId"))
                                            .addHeader("qos", message.headers().get("qos")));
                        }
                    }
                });
            }
        });
    }


}
