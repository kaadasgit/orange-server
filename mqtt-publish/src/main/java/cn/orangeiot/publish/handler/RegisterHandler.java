package cn.orangeiot.publish.handler;

import cn.orangeiot.publish.handler.event.EventHandler;
import cn.orangeiot.publish.handler.message.FuncHandler;
import cn.orangeiot.publish.handler.message.PublishHandler;
import cn.orangeiot.reg.EventbusAddr;
import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;
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

            //业务处理分发
            FuncHandler funcHandler=new FuncHandler(vertx,config);

            //网关事件上报
            EventHandler eventHandler=new EventHandler(vertx,config);

            //消息处理
            PublishHandler publishHandler=new PublishHandler(config,funcHandler,eventHandler);
            vertx.eventBus().consumer(PUBLISH_MSG,publishHandler::onMessage);//qos1和2回调
        } else {
            // failed!
            logger.error(res.cause().getMessage(), res.cause());
        }
    }

}
