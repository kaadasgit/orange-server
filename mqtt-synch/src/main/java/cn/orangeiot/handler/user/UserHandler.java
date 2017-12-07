package cn.orangeiot.handler.user;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2017-11-27
 */
public class UserHandler {
    private static Logger logger = LoggerFactory.getLogger(UserHandler.class);


    private Vertx vertx;

    private JsonObject config;

    public UserHandler(Vertx vertx, JsonObject config) {
        this.vertx = vertx;
        this.config = config;
    }


    /**
     * @Description 同步用户
     * @author zhang bo
     * @date 17-11-27
     * @version 1.0
     */
    public void onSynchUser(Message<JsonObject> message) {
        logger.info("==UserHandler=onSynchUser"+message);
        vertx.eventBus().send(config.getString("send_synch_user"), new JsonObject().put("key"
                , message.body().getString("username")).put("value", message.body().getString("salt") + "::"
                + message.body().getString("password")));
    }
}
