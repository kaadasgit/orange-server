package cn.orangeiot.handler.dao.user;

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
 * @date 2017-12-07
 */
public abstract class SynchUserDao {

    private static Logger logger = LoggerFactory.getLogger(SynchUserDao.class);

    /**
     * @Description 同步用户
     * @author zhang bo
     * @date 17-11-27
     * @version 1.0
     */
    public void onSynchUser(JsonObject message) {
        logger.info("==UserHandler=onSynchUser"+message);
        RedisClient.client.hset(RedisKeyConf.userAccount,message.getString("username")
                ,message.getString("password"),rs->{if(rs.failed())rs.cause().printStackTrace();});
    }
}
