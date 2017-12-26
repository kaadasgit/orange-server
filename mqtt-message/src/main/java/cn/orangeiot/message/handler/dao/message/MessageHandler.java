package cn.orangeiot.message.handler.dao.message;

import cn.orangeiot.message.Model.MqttQos;
import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2017-11-24
 */
public class MessageHandler {

    private static Logger logger = LoggerFactory.getLogger(MessageHandler.class);

    private Vertx vertx;

    private JsonObject config;

    public static Map<Long, AtomicInteger> numMap = new ConcurrentHashMap<>();//记录重发次数

    public MessageHandler(Vertx vertx, JsonObject config) {
        this.config = config;
        this.vertx = vertx;
    }

    /**
     * @Description 接收消息
     * @author zhang bo
     * @date 17-11-24
     * @version 1.0
     */
    public void onReceMessage(Message<JsonObject> message) {
        logger.info("==MessageHandelr=onReceMessage==params:" + message.body());

    }


    /**
     * @Description 发送消息
     * @author zhang bo
     * @date 17-11-24
     * @version 1.0
     */
    public void onSendMessage(Message<JsonObject> message) {
        logger.info("==MessageHandelr=onSendMessage==params:" + message.body() + "====header:" + message.headers());
        if (Objects.nonNull(message)) {
            if (message.body().getInteger("qos") == MqttQos.AT_LEAST_ONCE.getValue()
                    || message.body().getInteger("qos") == MqttQos.EXACTLY_ONCE.getValue()) {//qos为1或者2开始

                //重发机制
                AtomicInteger atomicInteger = new AtomicInteger(0);
                long timeId = vertx.setPeriodic(config.getLong("send_reply_time"), rs -> {
                    vertx.eventBus().send(config.getString("send_publishMessage"), message.body());//向broker发送消息
                    MessageHandler.numMap.get(rs).getAndIncrement();//原子自增
                    if (MessageHandler.numMap.get(rs).intValue() == config.getInteger("send_reply_num")) {//达到重发次数
                        vertx.cancelTimer(rs);//取消周期定时
                        MessageHandler.numMap.remove(rs);
                        saveOfflineMsg(message);//存储离线消息
                    }
                });
                numMap.put(timeId, atomicInteger);//存储绑定关系

                vertx.eventBus().send(config.getString("send_publishMessage"), message.body(),
                        new DeliveryOptions().addHeader("messageId",String.valueOf(timeId)));//向broker发送消息包含头部
            } else {
                vertx.eventBus().send(config.getString("send_publishMessage"), message.body());//向broker发送消息
            }
        }
    }

    /**
     * @Description qos1 接收 puback,qos2 接受 PUBCOMP  回调
     * @author zhang bo
     * @date 17-11-24
     * @version 1.0
     */
    public void onCallBackMsg(Message<JsonObject> message) {
        logger.info("==MessageHandelr=onCallBackMsg==params:" + message.body() + "====header:" + message.headers());
        if (Objects.nonNull(message.headers().get("messageId"))) {
            numMap.remove(Long.parseLong(message.headers().get("messageId")));
            vertx.cancelTimer(Long.parseLong(message.headers().get("messageId")));
            message.reply(true);
        } else {
            message.reply(false);
        }
    }


    /**
     * @Description 获取用户所有离线消息
     * @author zhang bo
     * @date 17-11-24
     * @version 1.0
     */
    public void onGetUserMsgAll(Message message) {
        logger.info("==MessageHandelr=onGetUserMsgAll==params:" + message.body() + "====header:" + message.headers());
        vertx.eventBus().send(config.getString("send_getUserMsgAll"),message,(AsyncResult<Message<JsonArray>> rs)->{
            message.reply(rs.result().body());
        });
    }


    /**
     * @Description 存储离线消息
     * @author zhang bo
     * @date 17-11-24
     * @version 1.0
     */
    public void saveOfflineMsg(Message message) {
        vertx.eventBus().send(config.getString("send_saveOfflineMessage"), message);
    }

}
