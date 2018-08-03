package cn.orangeiot.mqtt.log.handler;

import cn.orangeiot.reg.message.MessageAddr;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.kafka.client.producer.RecordMetadata;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-07-25
 */
public interface LogHandler extends MessageAddr {

    void writeLog(String msg, int msgId, long timeId, String topic, int partition, Handler<AsyncResult<Boolean>> handler);

    void writeLog(String msg, int msgId, long timeId, String topic, int partition);

    void writeLog(String msg, int msgId, long timeId, String topic, Handler<AsyncResult<Boolean>> handler);

    void writeLog(String msg, int msgId, long timeId, String topic);

    void writeLog(Message<JsonObject> message);

    void readLog(String topic, String custor);

    void consumeLog(int msgId, String topic, Message<JsonObject> message);

    void deleteLog(String topic);

}
