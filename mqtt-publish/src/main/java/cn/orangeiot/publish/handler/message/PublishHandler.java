package cn.orangeiot.publish.handler.message;

import cn.orangeiot.common.genera.ErrorType;
import cn.orangeiot.common.utils.DataType;
import cn.orangeiot.common.utils.StatusCode;
import cn.orangeiot.common.verify.VerifyParamsUtil;
import cn.orangeiot.publish.handler.event.EventHandler;
import cn.orangeiot.reg.gateway.GatewayAddr;
import cn.orangeiot.reg.message.MessageAddr;
import io.vertx.core.AsyncResult;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import javax.xml.crypto.Data;
import java.util.Objects;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2017-12-11
 */
public class PublishHandler implements MessageAddr {

    private static Logger logger = LogManager.getLogger(PublishHandler.class);

    private JsonObject conf;

    private FuncHandler funcHandler;

    private EventHandler eventHandler;

    private Vertx vertx;

    private final String USER_PREFIX = "app:";//用户前缀

    private final String GATEWAY_PREFIX = "gw:";//网关前缀

    private final String TOPIC_USER_REQUEST_SUFFIX = "/func";//用户后缀

    private final String TOPIC_USER_RPC_WITH_GATEWAY_REQSUFFIX = "/call";//用户与网关rpc通信请求后缀

    private final String TOPCI_GATEWAY_EVENT_SUFFIX = "/event";//网关事件后缀

    private final String TOPIC_USER_RPC_WITH_GATEWAY_REP_SUFFIX = "/reply";//网关与用户rpc通信响应后缀

    public PublishHandler(JsonObject conf, FuncHandler funcHandler, EventHandler eventHandler, Vertx vertx) {
        this.conf = conf;
        this.funcHandler = funcHandler;
        this.eventHandler = eventHandler;
        this.vertx = vertx;
    }

//    /**
//     * @Description 消息处理
//     * @author zhang bo
//     * @date 17-12-11
//     * @version 1.0
//     */
//    @SuppressWarnings("Duplicates")
//    public void onMessage(Message<JsonObject> message) {
//        logger.debug("==PublishHandler=onMessage==params -> " + message.body().toString());
//        VerifyParamsUtil.verifyParams(message.body(), new JsonObject().put("userId", DataType.STRING)
//                .put("deviceId", DataType.STRING).put("gwId", DataType.STRING).put("topicName", DataType.STRING)
//                .put("clientId", DataType.STRING), (AsyncResult<JsonObject> rs) -> {
//            if (rs.failed()) {
//                //app业务
//                if (Objects.nonNull(message.body().getValue("topicName")) && message.body().getString("topicName").equals(conf.getString("app_fuc_message"))) {
//                    funcHandler.onMessage(message, (AsyncResult<JsonObject> returnData) -> {
//                        if (returnData.failed()) {
//                            message.reply(new JsonObject().put("code", 404).put("msg", returnData.cause().getMessage())
//                                            .put("topicName", conf.getString("reply_message").replace("clientId",
//                                                    message.body().getString("uid"))),
//                                    new DeliveryOptions().addHeader("messageId", message.headers().get("messageId"))
//                                            .addHeader("qos", message.headers().get("qos")));
//                        } else {
//                            message.reply(returnData.result().put("topicName", conf.getString("reply_message").replace("clientId",
//                                    message.body().getString("uid"))),
//                                    new DeliveryOptions().addHeader("messageId", message.headers().get("messageId"))
//                                            .addHeader("qos", message.headers().get("qos")));
//                        }
//                    });
//                } else if (Objects.nonNull(message.body().getValue("topicName")) && message.body().getString("topicName").indexOf("/event") >= 0) {//网关事件上报
//                    eventHandler.onEventMessage(message, as -> {
//                        if (as.failed()) {
//                            logger.error(as.cause().getMessage());
////                            if (StringUtils.isNotBlank(as.cause().getMessage()))
////                                message.reply(new JsonObject().put("code", 401));//参数校验失败
////                            else
//                            message.reply(null);
//                        } else {
//                            conf.getString("app_fuc_message");
//                            message.reply(as.result(),
//                                    new DeliveryOptions().addHeader("messageId", message.headers().get("messageId"))
//                                            .addHeader("qos", message.headers().get("qos")));
//                        }
//                    });
//                } else if (Objects.nonNull(message.body().getValue("topicName")) && message.body().getString("topicName").indexOf("/rpc/call") > 0) {
//                    funcHandler.onMessage(message, (AsyncResult<JsonObject> returnData) -> {
//                        if (returnData.failed()) {
//                            returnData.cause().printStackTrace();
//                        } else {
//                            message.reply(null);
//                            DeliveryOptions deliveryOptions = new DeliveryOptions().addHeader("uid"
//                                    , "gw:" + returnData.result().getString("gwId"))
//                                    .addHeader("topic", MessageAddr.SEND_GATEWAY_REPLAY.replace("gwId"
//                                            , returnData.result().getString("gwId")))
//                                    .addHeader("messageId", message.headers().get("messageId"))
//                                    .addHeader("qos", message.headers().get("qos"));
//                            returnData.result().remove("topicName");
//                            returnData.result().remove("clientId");
//                            vertx.eventBus().send(MessageAddr.class.getName() + SEND_UPGRADE_MSG, returnData.result()
//                                    , deliveryOptions);
//                        }
//                    });
//                } else {
//                    message.reply(new JsonObject().put("code", 401)
//                            , new DeliveryOptions().addHeader("messageId", message.headers().get("messageId"))
//                                    .addHeader("qos", message.headers().get("qos")));//参数校验失败
//                }
//            } else {
//                String flag = rs.result().getString("clientId").split(":")[0];
//                funcHandler.onRpcMessage(message, as -> {
//                    if (as.failed()) {
//                        logger.error(as.cause().getMessage(), as);
//                        message.reply(null);
//                    } else {
//                        if (Objects.nonNull(message.body().getValue("topicName")) && message.body().getString("topicName").indexOf("/event") >= 0) {
//                            eventHandler.onEventMessage(message, eventRs -> {
//                                if (eventRs.failed()) {
//                                    logger.error(eventRs.cause().getMessage());
//                                    message.reply(null);
//                                } else {
//                                    conf.getString("app_fuc_message");
//                                    message.reply(eventRs.result(),
//                                            new DeliveryOptions().addHeader("messageId", message.headers().get("messageId"))
//                                                    .addHeader("qos", message.headers().get("qos")));
//                                }
//                            });
//                        } else if (flag.equals("app")) {//app 发送
//                            message.reply(new JsonObject().put("flag", true).put("topicName", conf.getString("repeat_message").replace("gwId",
//                                    message.body().getString("gwId"))),
//                                    new DeliveryOptions().addHeader("messageId", message.headers().get("messageId"))
//                                            .addHeader("qos", message.headers().get("qos")));
//                        } else {//gw 发送
//                            //reply回复app
//                            message.reply(new JsonObject().put("flag", true).put("topicName", conf.getString("reply_message").replace("clientId",
//                                    message.body().getString("userId"))),
//                                    new DeliveryOptions().addHeader("messageId", message.headers().get("messageId"))
//                                            .addHeader("qos", message.headers().get("qos")));
//                        }
//                    }
//                });
//            }
//        });
//    }


    /**
     * @Description 错误主题
     * @author zhang bo
     * @date 18-9-30
     * @version 1.0
     */
    public void failTopic(Message<JsonObject> message) {
        message.reply(message.body().put("returnCode", StatusCode.FAIL_TOPCINAME)
                , new DeliveryOptions().addHeader("messageId", message.headers().get("messageId"))
                        .addHeader("qos", message.headers().get("qos")));//参数校验失败
    }

    /**
     * @Description 消息处理
     * @author zhang bo
     * @date 17-12-11
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void onProcessMssage(Message<JsonObject> message) {
        logger.debug("==PublishHandler=onMessage==params -> " + message.body().toString());
        VerifyParamsUtil.verifyParams(message.body(), new JsonObject().put("topicName", DataType.STRING)
                .put("clientId", DataType.STRING), (AsyncResult<JsonObject> rs) -> {
            if (rs.failed()) {//數據校驗失敗
                message.reply(message.body().put("returnCode", StatusCode.PARAMS_FIAL)
                        , new DeliveryOptions().addHeader("messageId", message.headers().get("messageId"))
                                .addHeader("qos", message.headers().get("qos")));//参数校验失败
            } else {
                if (rs.result().getString("clientId").indexOf(USER_PREFIX) >= 0) {//用戶

                    processUser(rs.result(), message.headers(), message);

                } else if (rs.result().getString("clientId").indexOf(GATEWAY_PREFIX) >= 0) {//網關

                    processGateway(rs.result(), message.headers(), message);

                } else {//非法
                    message.reply(message.body().put("returnCode", StatusCode.EXCEPTION_SYSTEM_ACCOUNT)
                            , new DeliveryOptions().addHeader("messageId", message.headers().get("messageId"))
                                    .addHeader("qos", message.headers().get("qos")));//参数校验失败
                }
            }
        });
    }


    /**
     * @Description 獲取client id
     * @author zhang bo
     * @date 18-9-30
     * @version 1.0
     */
    public String getClientId(String client) {
        return client.split(":")[1];
    }

    /**
     * @param jsonObject 接收client 数据
     * @param map        接收client header 数据
     * @param message    消息reply通道 message.reply
     * @Description 用户处理
     * @author zhang bo
     * @date 18-9-30
     * @version 1.0
     */
    public void processUser(JsonObject jsonObject, MultiMap map, Message<JsonObject> message) {
        if (jsonObject.getString("topicName").indexOf(TOPIC_USER_REQUEST_SUFFIX) > 0) {//用户请求响应
            VerifyParamsUtil.verifyParams(message.body(), new JsonObject().put("func", DataType.STRING), res -> {
                if (res.failed()) {
                    logger.warn("data params is fail , data -> {}" + message.body());
                    message.reply(new JsonObject().put("code", StatusCode.Not_FOUND).put("msg", "params is fail")
                                    .put("topicName", conf.getString("reply_message").replace("clientId",
                                            getClientId(message.body().getString("clientId")))).put("func", message.body().getString("func")),
                            new DeliveryOptions().addHeader("messageId", message.headers().get("messageId"))
                                    .addHeader("qos", message.headers().get("qos")));
                } else {
                    funcHandler.onMessage(message, (AsyncResult<JsonObject> returnData) -> {
                        if (returnData.failed()) {
                            logger.warn("data params is fail , data -> {}" + message.body());
                            message.reply(new JsonObject().put("code", StatusCode.Not_FOUND).put("msg", returnData.cause().getMessage())
                                            .put("topicName", conf.getString("reply_message").replace("clientId",
                                                    getClientId(res.result().getString("clientId")))).put("func", res.result().getString("func")),
                                    new DeliveryOptions().addHeader("messageId", message.headers().get("messageId"))
                                            .addHeader("qos", message.headers().get("qos")));
                        } else {
                            message.reply(returnData.result().put("topicName", conf.getString("reply_message").replace("clientId",
                                    getClientId(res.result().getString("clientId")))).put("func", res.result().getString("func")),
                                    new DeliveryOptions().addHeader("messageId", message.headers().get("messageId"))
                                            .addHeader("qos", message.headers().get("qos")));
                        }
                    });
                }
            });
        } else if (jsonObject.getString("topicName").indexOf(TOPIC_USER_RPC_WITH_GATEWAY_REQSUFFIX) > 0) {//rpc 通信
            VerifyParamsUtil.verifyParams(message.body(), new JsonObject().put("gwId", DataType.STRING)
                    .put("userId", DataType.STRING), res -> {
                if (res.failed()) {
                    logger.warn("data params is fail , data -> {}" + message.body());
                    message.reply(new JsonObject().put("code", StatusCode.Not_FOUND).put("msg", "params is fail")
                                    .put("topicName", conf.getString("reply_message").replace("clientId",
                                            getClientId(message.body().getString("clientId")))).put("func", "RPC"),
                            new DeliveryOptions().addHeader("messageId", message.headers().get("messageId"))
                                    .addHeader("qos", message.headers().get("qos")));
                } else {
                    message.reply(null);//回复
                    vertx.eventBus().send(MessageAddr.class.getName() + SEND_UPGRADE_MSG, res.result()
                            , new DeliveryOptions().addHeader("uid", GATEWAY_PREFIX + res.result().getString("gwId")).addHeader("messageId", message.headers().get("messageId"))
                                    .addHeader("qos", message.headers().get("qos")).addHeader("topicName", conf.getString("repeat_message").replace("gwId"
                                            , res.result().getString("gwId"))));
                }
            });
        } else {
            this.failTopic(message);
        }

    }

    /**
     * @param jsonObject 接收client 数据
     * @param map        接收client header 数据
     * @param message    消息reply通道 message.reply
     * @Description 网关处理
     * @author zhang bo
     * @date 18-9-30
     * @version 1.0
     */
    public void processGateway(JsonObject jsonObject, MultiMap map, Message<JsonObject> message) {
        if (jsonObject.getString("topicName").indexOf(TOPIC_USER_RPC_WITH_GATEWAY_REP_SUFFIX) > 0) {//rpc响应
            VerifyParamsUtil.verifyParams(message.body(), new JsonObject().put("gwId", DataType.STRING).put("userId", DataType.STRING), res -> {
                if (res.failed()) {
                    logger.warn("data params is fail , data -> {}" + message.body());
                    message.reply(message.body().put("returnCode", StatusCode.PARAMS_FIAL).put("func", "RPC")
                                    .put("topicName", conf.getString("repeat_message").replace("gwId",
                                            getClientId(message.body().getString("clientId"))))
                            , new DeliveryOptions().addHeader("messageId", message.headers().get("messageId"))
                                    .addHeader("qos", message.headers().get("qos")));//参数校验失败
                } else {
                    funcHandler.onRpcGatewayResponseMessage(message, as -> {
                        if (as.failed()) {
                            logger.error(as.cause().getMessage(), as);
                            message.reply(null);
                        } else {
                            message.reply(null);
                            vertx.eventBus().send(MessageAddr.class.getName() + SEND_ADMIN_MSG, message.body(),
                                    new DeliveryOptions().addHeader("topicName", conf.getString("reply_message").replace("clientId", message.body().getString("userId"))
                                    ).addHeader("uid", USER_PREFIX + message.body().getString("userId")).addHeader("messageId", message.headers().get("messageId"))
                                            .addHeader("qos", message.headers().get("qos")));
                        }
                    });
                }
            });
        } else if (jsonObject.getString("topicName").indexOf(TOPCI_GATEWAY_EVENT_SUFFIX) > 0) {//网关事件
            VerifyParamsUtil.verifyParams(message.body(), new JsonObject().put("gwId", DataType.STRING), res -> {
                if (res.failed()) {
                    logger.warn("data params is fail , data -> {}" + message.body());
                    message.reply(message.body().put("returnCode", StatusCode.PARAMS_FIAL).put("func", "EVENT")
                                    .put("topicName", conf.getString("repeat_message").replace("gwId",
                                            getClientId(message.body().getString("clientId"))))
                            , new DeliveryOptions().addHeader("messageId", message.headers().get("messageId"))
                                    .addHeader("qos", message.headers().get("qos")));//参数校验失败
                } else {
                    eventHandler.onEventMessage(message, as -> {
                        message.reply(null);
                        if (as.failed()) logger.error(as.cause().getMessage());
                    });
                }
            });
        } else {
            this.failTopic(message);
        }

    }


}
