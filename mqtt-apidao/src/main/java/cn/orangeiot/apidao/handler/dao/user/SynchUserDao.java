package cn.orangeiot.apidao.handler.dao.user;

import cn.orangeiot.apidao.client.RedisClient;
import cn.orangeiot.apidao.conf.RedisKeyConf;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        RedisClient.client.hset(RedisKeyConf.USER_ACCOUNT,message.getString("username")
                ,message.getString("userPwd"),rs->{if(rs.failed())rs.cause().printStackTrace();});
    }


    /**
     * @Description 同步用户
     * @author zhang bo
     * @date 17-11-27
     * @version 1.0
     */
    public void onSynchUserInfo(JsonObject message) {
        logger.info("==UserHandler=onSynchUserInfo"+message);
        RedisClient.client.hset(RedisKeyConf.USER_INFO,message.getString("_id")
                ,message.toString(),rs->{if(rs.failed())rs.cause().printStackTrace();});
    }

}
