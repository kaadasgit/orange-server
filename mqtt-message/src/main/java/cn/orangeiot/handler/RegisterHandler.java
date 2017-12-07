package cn.orangeiot.handler;

import cn.orangeiot.handler.message.MessageHandler;
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

            //消息处理
            MessageHandler messageHandler=new MessageHandler(vertx,config);
            vertx.eventBus().consumer(config.getString("consumer_receMessage"),messageHandler::onReceMessage).exceptionHandler(th->{
                th.printStackTrace();
            });//接收消息
            vertx.eventBus().consumer(config.getString("consumer_messageCallBack"),messageHandler::onCallBackMsg);//qos1和2回调
            vertx.eventBus().consumer(config.getString("consumer_getUserMsgAll"),messageHandler::onCallBackMsg);//获取用户离线消息
            vertx.eventBus().consumer(config.getString("consumer_sendMessage"),messageHandler::onSendMessage).exceptionHandler(th->{
                th.printStackTrace();
            });//发送消息
        } else {
            // failed!
            logger.fatal(res.cause().getMessage(), res.cause());
        }
    }

}
