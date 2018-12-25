package cn.orangeiot.auth.handler.user;

import cn.orangeiot.common.options.SendOptions;
import cn.orangeiot.reg.user.UserAddr;
import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2017-12-11
 */
public class UserHandler implements UserAddr {
    private static Logger logger = LogManager.getLogger(UserHandler.class);


    private Vertx vertx;

    private JsonObject config;

    public UserHandler(Vertx vertx, JsonObject config) {
        this.vertx = vertx;
        this.config = config;
    }

    /**
     * @Description 手机登录
     * @author zhang bo
     * @date 17-12-11
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void onByTelMessage(Message<JsonObject> message) {
        vertx.eventBus().send(UserAddr.class.getName() + VERIFY_TEL, message.body(), SendOptions.getInstance()
                , (AsyncResult<Message<JsonObject>> rs) -> {
                    if (rs.failed()) {
                        logger.error(rs.cause().getMessage(), rs.cause());
                        message.reply(null);
                    } else {
                        message.reply(rs.result().body());
                    }
                });
    }


    /**
     * @Description mail登录
     * @author zhang bo
     * @date 17-12-11
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void onByMailMessage(Message<JsonObject> message) {
        vertx.eventBus().send(UserAddr.class.getName() + VERIFY_MAIL, message.body(), SendOptions.getInstance()
                , (AsyncResult<Message<JsonObject>> rs) -> {
                    if (rs.failed()) {
                        logger.error(rs.cause().getMessage(), rs.cause());
                        message.reply(null);
                    } else {
                        message.reply(rs.result().body());
                    }
                });
    }
}
