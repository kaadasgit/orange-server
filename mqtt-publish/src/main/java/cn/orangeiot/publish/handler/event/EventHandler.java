package cn.orangeiot.publish.handler.event;

import cn.orangeiot.common.options.SendOptions;
import cn.orangeiot.common.utils.DataType;
import cn.orangeiot.common.verify.VerifyParamsUtil;
import cn.orangeiot.reg.event.EventAddr;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

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

    public EventHandler(Vertx vertx, JsonObject jsonObject) {
        this.vertx = vertx;
        this.jsonObject = jsonObject;
    }

    /**
     * @Description 事件上报处理
     * @author zhang bo
     * @date 18-1-16
     * @version 1.0
     */
    public void onEventMessage(Message<JsonObject> message, Handler<AsyncResult<JsonObject>> handler) {
        logger.info("==EventHandler=onEventMessage params -> " + message.body());
        VerifyParamsUtil.verifyParams(message.body(), new JsonObject().put("devid", DataType.STRING)
                , (AsyncResult<JsonObject> rs) -> {
                    if (rs.failed()) {
                        message.reply(new JsonObject().put("code", 401));//参数校验失败
                    } else {
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
                    }
                });
    }
}
