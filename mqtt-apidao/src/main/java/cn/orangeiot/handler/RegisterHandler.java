package cn.orangeiot.handler;

import cn.orangeiot.client.MongoClient;
import cn.orangeiot.client.RedisClient;
import cn.orangeiot.handler.dao.message.MessageDao;
import cn.orangeiot.handler.dao.topic.TopicDao;
import cn.orangeiot.handler.dao.user.UserDao;
import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * @author zhang bo
 * @version 1.0 集群的handler事件注册
 * @Description
 * @date 2017-11-23
 */
public class RegisterHandler {

    private static Logger logger = LoggerFactory.getLogger(RegisterHandler.class);

    private JsonObject config;

    public RegisterHandler(JsonObject config) {
        this.config=config;
    }

    /**
     * @Description 注册事件
     * @author zhang bo
     * @date 17-11-23
     * @version 1.0
     */
    public void consumer(AsyncResult<Vertx> res){
        if (res.succeeded()) {
            Vertx vertx = res.result();


            //注册mongoclient
            MongoClient mongoClient =new MongoClient();
            mongoClient.mongoConf(vertx);

            //注册redisclient
            RedisClient redisClient=new RedisClient();
            redisClient.redisConf(vertx);

            //topic处理
            TopicDao topicHandler=new TopicDao();
            vertx.eventBus().consumer(config.getString("consumer_saveTopic"),topicHandler::saveTopic);
            vertx.eventBus().consumer(config.getString("consumer_delTopic"),topicHandler::saveTopic);

            //离线消息储存
            MessageDao messageHandler=new MessageDao();
            vertx.eventBus().consumer(config.getString("consumer_saveOfflineMessage"),messageHandler::onSaveOfflineMsg);

            //连接处理
            UserDao userDao=new UserDao();
            vertx.eventBus().consumer(config.getString("consumer_connect_dao"),userDao::getUser);

        } else {
            // failed!
            logger.fatal(res.cause().getMessage(), res.cause());
        }
    }



}
