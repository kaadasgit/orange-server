package cn.orangeiot.mqtt.log;

import cn.orangeiot.common.utils.DataType;
import cn.orangeiot.common.verify.VerifyParamsUtil;
import cn.orangeiot.mqtt.log.handler.impl.LogHandlerImpl;
import cn.orangeiot.reg.log.LogAddr;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-07-26
 */
public class Spi implements LogAddr {


    /**
     * @Description 注册事件
     * @author zhang bo
     * @date 18-7-26
     * @version 1.0
     * String msg, int msgId, long timeId, String topic
     */
    public void registerEvent(Vertx vertx) {
        vertx.eventBus().consumer(LogAddr.class.getName() + WRITE_LOG, (Message<JsonObject> rs) -> new LogHandlerImpl().writeLog(rs));
        vertx.eventBus().consumer(LogAddr.class.getName() + READ_LOG, (Message<JsonObject> rs) -> new LogHandlerImpl().readLog(rs));
        vertx.eventBus().consumer(LogAddr.class.getName() + CONSUME_LOG, (Message<JsonObject> rs) -> new LogHandlerImpl().consumeLog(rs));
        vertx.eventBus().consumer(LogAddr.class.getName() + MSG_EXISTS, (Message<JsonObject> rs) -> new LogHandlerImpl().msgExists(rs));
        vertx.eventBus().consumer(LogAddr.class.getName() + SAVE_PUBREL, (Message<JsonObject> rs) -> new LogHandlerImpl().saveRelMsgId(rs));
        vertx.eventBus().consumer(LogAddr.class.getName() + SEND_PUBREL, (Message<JsonObject> rs) -> new LogHandlerImpl().readRelMsgId(rs));
        vertx.eventBus().consumer(LogAddr.class.getName() + CONSUME_PUBREL, (Message<JsonObject> rs) -> new LogHandlerImpl().consumerRel(rs));
    }


}
