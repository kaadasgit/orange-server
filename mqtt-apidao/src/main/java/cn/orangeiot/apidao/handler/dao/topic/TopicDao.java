package cn.orangeiot.apidao.handler.dao.topic;

import cn.orangeiot.apidao.client.RedisClient;
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
 * @date 2017-11-23
 */
public class TopicDao {

    private static Logger logger = LogManager.getLogger(TopicDao.class);

    /**
     * @Description 保存主题
     * @author zhang bo
     * @date 17-11-23
     * @version 1.0
     */
    public void saveTopic(Message<JsonObject> message){
        if(Objects.nonNull(message.body())){
            RedisClient.client.hsetnx(RedisKeyConf.SUBSCRIBE_kEY, message.body().getString("topicName"), message.body().getString("uid"), rs -> {
                if (rs.failed()) logger.error(rs.cause().getMessage(), rs);
            });//保存订阅主题和发布者

            RedisClient.client.hset(RedisKeyConf.SUBSCRIBE_CLIENT_kEY + message.body().getString("topicName"), message.body().getString("uid"), "", rs -> {
                if (rs.failed()) logger.error(rs.cause().getMessage(), rs);
            });//订阅主题的用户集合

            message.reply(true);
        }else{
            message.reply(false);
        }

    }


    /**
     * @Description 删除主题
     * @author zhang bo
     * @date 17-11-23
     * @version 1.0
     */
    public void delTopic(Message<JsonObject> message){
        if(Objects.nonNull(message.body())){
            RedisClient.client.hdel(RedisKeyConf.SUBSCRIBE_CLIENT_kEY + message.body().getString("topicName"), message.body().getString("uid"), rs -> {
                if (rs.failed()) logger.error(rs.cause().getMessage(), rs);
            });//删除订阅账户

            RedisClient.client.hget(RedisKeyConf.SUBSCRIBE_kEY, message.body().getString("topicName"), rs -> {//查询主题发布者
                if (rs.failed()) {
                    logger.error(rs.cause().getMessage(), rs);
                } else {
                    if (Objects.nonNull(rs.result()) && rs.result().toString().equals(message.body().getString("uid"))) {//如果是主题发布者
                        RedisClient.client.hdel(RedisKeyConf.SUBSCRIBE_kEY,  message.body().getString("topicName"), as -> {
                            if (as.failed()) logger.error(as.cause().getMessage(), as);
                        });
                        RedisClient.client.del(RedisKeyConf.SUBSCRIBE_CLIENT_kEY +  message.body().getString("topicName"), as -> {
                            if (as.failed()) logger.error(as.cause().getMessage(), as);
                        });
                    }
                }
            });

            message.reply(true);
        }else{
            message.reply(false);
        }

    }
}
