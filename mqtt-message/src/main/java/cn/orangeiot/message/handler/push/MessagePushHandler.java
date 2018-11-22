package cn.orangeiot.message.handler.push;

import cn.orangeiot.common.constant.HttpAttrType;
import cn.orangeiot.message.constant.ConstantConf;
import cn.orangeiot.message.handler.client.PushClient;
import cn.orangeiot.reg.message.MessageAddr;
import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Base64;
import java.util.Objects;
import java.util.UUID;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-05-21
 */
public class MessagePushHandler implements MessageAddr {

    private static Logger logger = LogManager.getLogger(MessagePushHandler.class);

    private String Authorization;

    private Vertx vertx;

    public MessagePushHandler(JsonObject conf, Vertx vertx) throws Exception {
        String auth = ConstantConf.AUTH_PRE + conf.getString("jpush_appkey") + ":" + conf.getString("jpush_secret");
        Authorization = Base64.getEncoder().encodeToString(auth.getBytes("UTF-8"));
        this.vertx = vertx;
    }


    /**
     * @Description 發送application通知
     * @author zhang bo
     * @date 18-5-22
     * @version 1.0
     */
    public void sendPushNotify(Message<JsonObject> message) {
        logger.debug("params -> {}", message.body());
        vertx.eventBus().send(MessageAddr.class.getName() + GET_PUSHID, message.body(), (AsyncResult<Message<JsonObject>> as) -> {
            if (as.failed()) {
                logger.error(as.cause().getMessage(), as);
            } else {
                if (Objects.nonNull(as.result()) && as.result().body() != null) {
                    if (as.result().body().getInteger("type") == 1) {//ios
                        sendAPNS(message.body(), as.result().body());
                    } else {//android
                        sendJpush(message.body(), as.result().body());
                    }
                } else {
                    logger.warn("pushId is null , not upload , uid -> {}", message.body().getString("uid"));
                }
            }
        });
    }


    /**
     * @Description 發送音响通知
     * @author zhang bo
     * @date 18-5-22
     * @version 1.0
     */
    public void sendPushSoundNotify(Message<JsonObject> message) {
        logger.debug("params -> {}", message.body());
        vertx.eventBus().send(MessageAddr.class.getName() + GET_PUSHID, message.body(), (AsyncResult<Message<JsonObject>> as) -> {
            if (as.failed()) {
                logger.error(as.cause().getMessage(), as);
            } else {
                if (Objects.nonNull(as.result()) && as.result().body() != null) {
                    if (as.result().body().getInteger("type") == 1) {//ios
                        sendSoundAPNS(message.body(), as.result().body());
                    } else {//android
                        sendJpush(message.body(), as.result().body());
                    }
                } else {
                    logger.warn("pushId is null , not upload , uid -> {}", message.body().getString("uid"));
                }
            }
        });
    }


    /**
     * @Description 推送APNS服务器 音响
     * @author zhang bo
     * @date 18-5-28
     * @version 1.0
     */
    public void sendSoundAPNS(JsonObject message, JsonObject jpush) {
        if (jpush.getValue("VoIPId") != null) {
            JsonObject params = new JsonObject().put("aps", new JsonObject().put("alert", new JsonObject().put("body", message.getString("content"))
                    .put("title", message.getString("title")))).put("extras", message.getJsonObject("extras"));
            String uuid = UUID.randomUUID().toString();
            logger.info("request APNS sound apple uri /3/device/{} , uid -> {} ,  header.apns-id -> {} , bodyLength -> {}"
                    , jpush.getString("VoIPId"), message.getString("uid"), uuid, params.toString().length());
            PushClient.iosVOIPClient.post("/3/device/" + jpush.getString("VoIPId"))
                    .putHeader(HttpAttrType.CONTENT_TYPE_JSON.getKey(), HttpAttrType.CONTENT_TYPE_JSON.getValue())
                    .putHeader("apns-topic", ConstantConf.APNS_TOPIC_VOIP)
                    .putHeader("apns-id", uuid)
                    .putHeader("Content-length", String.valueOf(params.toString().getBytes().length))
                    .sendJsonObject(params, rs -> {
                        if (rs.failed()) {
                            logger.error(rs.cause().getMessage(), rs);
                        } else {
                            logger.info("request APNS sound apple result status -> {} ,  header.apns-id -> {} , requestUid -> {}", rs.result().statusCode(),
                                    rs.result().getHeader("apns-id"), message.getString("uid"));
                        }
                    });
        } else {
            logger.error("sendSoundAPNS VoIPId is null , uid -> {}", message.getString("uid"));
        }
    }


    /**
     * @Description 推送極光
     * @author zhang bo
     * @date 18-5-28
     * @version 1.0
     */
    public void sendJpush(JsonObject message, JsonObject jpush) {
        if (jpush.getValue("JPushId") != null) {
            JsonObject params = new JsonObject().put("platform", "all")
                    .put("audience", new JsonObject().put("segment"
                            , new JsonArray().add(jpush.getString("JPushId"))))
                    .put("notification", new JsonObject().put("alert", message.getString("content"))
                            .put("title", message.getString("title")).put("extras", message.getJsonObject("extras")))
                    .put("options", new JsonObject().put("time_to_live", message.getInteger("time_to_live")));
            ;//最大十天有效時間,单位second

            logger.info("request android uri /v3/push , uid -> {} ,  header.Authorization -> {} , bodyLength -> {}", message.getString("uid")
                    , jpush.getString("JPushId"), Authorization, params.toString().length());
            PushClient.androidClient.post("/v3/push")
                    .putHeader(HttpAttrType.CONTENT_TYPE_JSON.getKey(), HttpAttrType.CONTENT_TYPE_JSON.getValue())
                    .putHeader("Authorization", Authorization)
                    .sendJsonObject(params, rs -> {
                        if (rs.failed()) {
                            logger.error(rs.cause().getMessage(), rs);
                        } else {
                            logger.info("request android result url /v3/push , JPushId -> {} , result -> {} , requestUid -> {}",
                                    jpush.getString("JPushId"), rs.result().body().toString(), message.getString("uid"));
                        }
                    });
        }else{
            logger.error("sendJpush JPushId is null , uid -> {}", message.getString("uid"));
        }
    }


    /**
     * @Description 推送APNS服务器
     * @author zhang bo
     * @date 18-5-28
     * @version 1.0
     */
    public void sendAPNS(JsonObject message, JsonObject jpush) {
        if (jpush.getValue("JPushId") != null) {
            JsonObject params = new JsonObject().put("aps", new JsonObject().put("alert", new JsonObject().put("body", message.getString("content"))
                    .put("title", message.getString("title"))).put("sound", "default")).put("extras", message.getJsonObject("extras"));
            String uuid = UUID.randomUUID().toString();
            logger.info("request APNS push apple uri /3/device/{} , uid -> {} ,  header.apns-id -> {} , bodyLength -> {}"
                    , jpush.getString("JPushId"), message.getString("uid"), uuid, params.toString().length());
            PushClient.iosClient.post("/3/device/" + jpush.getString("JPushId"))
                    .putHeader(HttpAttrType.CONTENT_TYPE_JSON.getKey(), HttpAttrType.CONTENT_TYPE_JSON.getValue())
                    .putHeader("apns-topic", ConstantConf.APNS_TOPIC)
                    .putHeader("apns-id", uuid)
                    .putHeader("Content-length", String.valueOf(params.toString().getBytes().length))
                    .sendJsonObject(params, rs -> {
                        if (rs.failed()) {
                            logger.error(rs.cause().getMessage(), rs);
                        } else {
                            logger.info("request APNS push apple result status -> {} ,  header.apns-id -> {} , requestUid -> {}", rs.result().statusCode(),
                                    rs.result().getHeader("apns-id"), message.getString("uid"));
                        }
                    });
        }else{
            logger.error("sendAPNS JPushId is null , uid -> {}", message.getString("uid"));
        }
    }
}