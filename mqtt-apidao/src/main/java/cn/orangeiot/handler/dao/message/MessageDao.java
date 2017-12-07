package cn.orangeiot.handler.dao.message;

import cn.orangeiot.client.RedisClient;
import cn.orangeiot.conf.RedisKeyConf;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.Objects;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2017-11-24
 */
public class MessageDao {

    private static Logger logger = LoggerFactory.getLogger(MessageDao.class);

    /**
     * @Description 存储用户离线消息
     * @author zhang bo
     * @date 17-11-24
     * @version 1.
     */
    public void onSaveOfflineMsg(Message<JsonObject> message) {
        logger.info("==MessageHandler=onSaveOfflineMsg==parmas:" + message.body() + "====header:" + message.headers());
        if (Objects.nonNull(message)) {
            RedisClient.client.rpush(RedisKeyConf.userOfflineMessage + message.headers().get("uid"),
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
            RedisClient.client.lrange(RedisKeyConf.userOfflineMessage + message.headers().get("uid"),
                    0, -1, rs -> {
                        if (rs.failed()) rs.cause().printStackTrace();
                        else message.reply(rs.result());
                    });
        }
    }
}
