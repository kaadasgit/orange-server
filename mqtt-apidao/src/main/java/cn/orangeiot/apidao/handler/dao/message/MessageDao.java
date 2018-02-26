package cn.orangeiot.apidao.handler.dao.message;

import cn.orangeiot.apidao.client.RedisClient;
import cn.orangeiot.apidao.conf.Constant;
import cn.orangeiot.apidao.conf.RedisKeyConf;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.util.Objects;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2017-11-24
 */
public class MessageDao {

    private static Logger logger = LogManager.getLogger(MessageDao.class);

    /**
     * @Description 存储用户离线消息
     * @author zhang bo
     * @date 17-11-24
     * @version 1.
     */
    public void onSaveOfflineMsg(Message<JsonObject> message) {
        logger.info("==MessageHandler=onSaveOfflineMsg==parmas:" + message.body() + "====header:" + message.headers());
        if (Objects.nonNull(message)) {
            RedisClient.client.rpush(RedisKeyConf.USER_OFFLINE_MESSAGE + message.headers().get("uid"),
                    message.body().toString(), rs -> {
                        if (rs.failed()) rs.cause().printStackTrace();
                    });
        }
    }


    /**
     * @Description 获取用户离线消息
     * @author zhang bo
     * @date 17-11-24
     * @version 1.0
     */
    public void onGetOfflineMsg(Message<JsonObject> message) {
        logger.info("==MessageHandler=onSaveOfflineMsg==parmas:" + message.body() + "====header:" + message.headers());
        if (Objects.nonNull(message)) {
            RedisClient.client.lrange(RedisKeyConf.USER_OFFLINE_MESSAGE + message.headers().get("uid"),
                    0, -1, rs -> {
                        if (rs.failed()) rs.cause().printStackTrace();
                        else message.reply(rs.result());
                    });
        }
    }


    /**
     * @Description 缓存验证码
     * @author zhang bo
     * @date 17-12-12
     * @version 1.0
     */
    public void onSaveVerityCode(Message<JsonObject> message) {
        logger.info("==MessageHandler=onSaveVerityCode==parmas:" + message.body());
        RedisClient.client.setex(message.body().getString("tel"), Constant.VERITY_CODE_TIME,
                message.body().getString("verifyCode"), rs -> {
                    if (rs.failed()) rs.cause().printStackTrace();
                });
        RedisClient.client.hincrby(RedisKeyConf.VERIFY_CODE_COUNT, message.body().getString("tel"),
                1, rs -> {
                    if (rs.failed()) rs.cause().printStackTrace();
                });
    }


    /**
     * @Description 获取验证码发送次数
     * @author zhang bo
     * @date 17-12-12
     * @version 1.0
     */
    public void onGetCodeCount(Message<JsonObject> message) {
        logger.info("==MessageHandler=onGetCodeCount==params:" + message.body());
        RedisClient.client.hget(RedisKeyConf.VERIFY_CODE_COUNT, message.body().getString("tel"), rs -> {
            if (rs.failed()) {
                rs.cause().printStackTrace();
            } else {
                if(Objects.nonNull(rs.result()) && Integer.parseInt(rs.result())>message.body().getInteger("count")){
                    message.reply(false);
                }else{
                    message.reply(true);
                }
            }
        });
    }
}