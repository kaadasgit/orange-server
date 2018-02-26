package cn.orangeiot.auth.handler;

import cn.orangeiot.auth.handler.connect.ConnectHandler;
import cn.orangeiot.auth.handler.user.UserHandler;
import cn.orangeiot.reg.EventbusAddr;
import cn.orangeiot.reg.user.UserAddr;
import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * @author zhang bo
 * @version 1.0 集群的handler事件注册
 * @Description
 * @date 2017-11-23
 */
public class RegisterHandler implements EventbusAddr{

    private static Logger logger = LogManager.getLogger(RegisterHandler.class);

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

            //连接处理
            ConnectHandler connectHandler=new ConnectHandler(vertx,config);
            vertx.eventBus().consumer(config.getString("connectAuth"),connectHandler::onMessage);

            //连接处理
            UserHandler userHandler=new UserHandler(vertx,config);
            vertx.eventBus().consumer(UserAddr.class.getName()+LOGIN_TEL,userHandler::onByTelMessage);
            vertx.eventBus().consumer(UserAddr.class.getName()+LOGIN_MAIL,userHandler::onByMailMessage);
        } else {
            // failed!
            logger.error(res.cause().getMessage(), res.cause());
        }
    }

}
