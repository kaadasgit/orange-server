package cn.orangeiot.apidao.handler.dao.user;

import cn.orangeiot.apidao.client.RedisClient;
import cn.orangeiot.apidao.conf.RedisKeyConf;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.util.Objects;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2017-12-07
 */
public abstract class SynchUserDao {

    private static Logger logger = LogManager.getLogger(SynchUserDao.class);

    /**
     * @Description 同步用户
     * @author zhang bo
     * @date 17-11-27
     * @version 1.0
     */
    public void onSynchUser(JsonObject message) {
        logger.info("==UserHandler=onSynchUser" + message);
        RedisClient.client.hset(RedisKeyConf.USER_ACCOUNT, message.getString("username")
                , message.getString("userPwd"), rs -> {
                    if (rs.failed()) rs.cause().printStackTrace();
                });
    }


    /**
     * @Description 同步用户
     * @author zhang bo
     * @date 17-11-27
     * @version 1.0
     */
    public void onSynchUserInfo(JsonObject message) {
        logger.info("==UserHandler=onSynchUserInfo" + message);
        RedisClient.client.hset(RedisKeyConf.USER_INFO, message.getString("_id")
                , message.toString(), rs -> {
                    if (rs.failed()) rs.cause().printStackTrace();
                });
    }


    /**
     * @Description 同步用户修改的信息
     * @author zhang bo
     * @date 17-11-27
     * @version 1.0
     */
    public void onSynchUpdateUserInfo(JsonObject message) {
        logger.info("==UserHandler=onSynchUpdateUserInfo" + message);
        RedisClient.client.hget(RedisKeyConf.USER_INFO, message.getString("uid"), rs -> {
            if (rs.failed()) {
                rs.cause().printStackTrace();
            } else {
                if (Objects.nonNull(rs.result())) {
                    JsonObject jsonObject = new JsonObject(rs.result()).put("userPwd", message.getString("userPwd"));
                    RedisClient.client.hset(RedisKeyConf.USER_INFO, message.getString("uid")
                            , jsonObject.toString(), as -> {
                                if (as.failed()) as.cause().printStackTrace();
                            });
                }

            }
        });


    }

}
