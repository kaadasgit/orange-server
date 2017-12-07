package cn.orangeiot.handler.user;

import cn.orangeiot.client.RedisClient;
import cn.orangeiot.conf.RedisKeyConf;
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


    /**
     * @Description 保存用户
     * @author zhang bo
     * @date 17-11-23
     * @version 1.0
     */
    public void saveUser(Message<JsonObject> message){
        logger.info("==UserHandler=saveUser"+message);
        RedisClient.client.hset(RedisKeyConf.userAccount,message.body().getString("key")
                ,message.body().getString("value"),rs->{if(rs.failed())rs.cause().printStackTrace();});
    }
}
