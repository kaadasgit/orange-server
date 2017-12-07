package cn.orangeiot.handler;

import cn.orangeiot.client.MongoClient;
import cn.orangeiot.handler.connect.ConnectHandler;
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


            //注册redisclient
            MongoClient mongoClient =new MongoClient();
            mongoClient.mongoConf(vertx);

            //查找用户
            ConnectHandler connectHandler=new ConnectHandler(vertx,config);
            vertx.eventBus().consumer(config.getString("consumer_connect_dao"),connectHandler::getUser);
        } else {
            // failed!
            logger.fatal(res.cause().getMessage(), res.cause());
        }
    }



}
