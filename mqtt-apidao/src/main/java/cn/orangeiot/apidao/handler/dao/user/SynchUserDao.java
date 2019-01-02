package cn.orangeiot.apidao.handler.dao.user;

import cn.orangeiot.apidao.client.MongoClient;
import cn.orangeiot.apidao.client.RedisClient;
import cn.orangeiot.apidao.conf.RedisKeyConf;
import cn.orangeiot.common.constant.mongodb.KdsGatewayDeviceList;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.FindOptions;
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

    private final long DEFAULT_EXPIRE = 7 * 24 * 3600;//存活时间

    /**
     * @Description 同步用户
     * @author zhang bo
     * @date 17-11-27
     * @version 1.0
     */
    public void onSynchUser(JsonObject message) {
        logger.debug("==UserHandler=onSynchUser" + message);
        RedisClient.client.hset(RedisKeyConf.USER_ACCOUNT + message.getString("username")
                , RedisKeyConf.USER_VAL_TOKEN, message.getString("userPwd"), rs -> {
                    if (rs.failed()) logger.error(rs.cause().getMessage(), rs.cause());
                });
    }


    /**
     * @Description 同步相关信息
     * @author zhang bo
     * @date 17-11-27
     * @version 1.0
     */
    public void onGatewayInfo(JsonObject message) {
        logger.debug("==UserHandler=onSynchUser" + message);
        MongoClient.client.findWithOptions(KdsGatewayDeviceList.COLLECT_NAME, new JsonObject().put(KdsGatewayDeviceList.DEVICE_SN, message.getString("username")),
                new FindOptions().setFields(new JsonObject().put(KdsGatewayDeviceList.UID, 1).put(KdsGatewayDeviceList._ID, 0)), ars -> {
                    if (ars.failed()) {
                        logger.error(ars.cause().getMessage(), ars);
                    } else {
                        RedisClient.client.hmset(RedisKeyConf.USER_ACCOUNT + message.getString("username")
                                , new JsonObject().put(RedisKeyConf.USER_VAL_TOKEN, message.getString("userPwd"))
                                        .put(RedisKeyConf.BING_USER_INFO, new JsonArray(ars.result())), rs -> {
                                    if (rs.failed()) logger.error(rs.cause().getMessage(), rs.cause());
                                    else
                                        RedisClient.client.expire(RedisKeyConf.USER_ACCOUNT + message.getString("username"), DEFAULT_EXPIRE, times -> {
                                            if (times.failed()) logger.error(times.cause().getMessage(), times);
                                        });
                                });
                    }
                });
    }


    /**
     * @Description 同步用户
     * @author zhang bo
     * @date 17-11-27
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void onSynchUserInfo(JsonObject message, String token, Long liveTime) {
        logger.debug("==UserHandler=onSynchUserInfo" + message);
        RedisClient.client.hget(RedisKeyConf.USER_ACCOUNT + message.getString("_id"), RedisKeyConf.USER_VAL_TOKEN, ars -> {
            if (ars.failed()) logger.error(ars.cause().getMessage(), ars);
            else {
                if (Objects.nonNull(ars.result())) {
                    RedisClient.client.hmset(RedisKeyConf.USER_ACCOUNT + message.getString("_id"),
                            new JsonObject().put(RedisKeyConf.USER_VAL_INFO
                                    , message.toString()).put(RedisKeyConf.USER_VAL_TOKEN, token).put(RedisKeyConf.USER_VAL_OLDTOKEN, ars.result()), rs -> {
                                if (rs.failed()) logger.error(rs.cause().getMessage(), rs.cause());
                                else
                                    RedisClient.client.expire(RedisKeyConf.USER_ACCOUNT + message.getString("_id")
                                            , liveTime, times -> {
                                                if (times.failed())
                                                    logger.error(times.cause().getMessage(), times.cause());
                                            });
                            });
                } else {
                    RedisClient.client.hmset(RedisKeyConf.USER_ACCOUNT + message.getString("_id"),
                            new JsonObject().put(RedisKeyConf.USER_VAL_INFO
                                    , message.toString()).put(RedisKeyConf.USER_VAL_TOKEN, token), rs -> {
                                if (rs.failed()) logger.error(rs.cause().getMessage(), rs.cause());
                                else
                                    RedisClient.client.expire(RedisKeyConf.USER_ACCOUNT + message.getString("_id")
                                            , liveTime, times -> {
                                                if (times.failed())
                                                    logger.error(times.cause().getMessage(), times.cause());
                                            });
                            });
                }
            }
        });

    }

    /**
     * @Description 同步注册信息
     * @author zhang bo
     * @date 18-9-7
     * @version 1.0
     */
    public void onSynchRegisterUserInfo(JsonObject message) {
        logger.debug("==UserHandler=onSynchUserInfo" + message);
        RedisClient.client.hset(RedisKeyConf.USER_ACCOUNT + message.getString("_id"), RedisKeyConf.USER_VAL_INFO
                , message.toString(), rs -> {
                    if (rs.failed()) logger.error(rs.cause().getMessage(), rs.cause());
                });
    }


    /**
     * @Description 同步用户修改的信息
     * @author zhang bo
     * @date 17-11-27
     * @version 1.0
     */
    public void onSynchUpdateUserInfo(JsonObject message) {
        logger.debug("==UserHandler=onSynchUpdateUserInfo" + message);
        RedisClient.client.hget(RedisKeyConf.USER_ACCOUNT + message.getString("uid"), RedisKeyConf.USER_VAL_INFO, rs -> {
            if (rs.failed()) {
                logger.error(rs.cause().getMessage(), rs.cause());
            } else {
                if (Objects.nonNull(rs.result())) {
                    JsonObject jsonObject = new JsonObject(rs.result()).put("userPwd", message.getString("userPwd"));
                    RedisClient.client.hset(RedisKeyConf.USER_ACCOUNT + message.getString("uid"), RedisKeyConf.USER_VAL_INFO
                            , jsonObject.toString(), as -> {
                                if (as.failed()) logger.error(as.cause().getMessage(), as.cause());
                            });
                }

            }
        });


    }

}
