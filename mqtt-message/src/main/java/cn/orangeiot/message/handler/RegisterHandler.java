package cn.orangeiot.message.handler;

import cn.orangeiot.message.handler.client.MailClient;
import cn.orangeiot.message.handler.dao.message.MessageHandler;
import cn.orangeiot.reg.EventbusAddr;
import cn.orangeiot.reg.user.UserAddr;
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
public class RegisterHandler implements EventbusAddr{

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
//            MessageHandler messageHandler=new MessageHandler(vertx,config);
//            vertx.eventBus().consumer(config.getString("consumer_receMessage"),messageHandler::onReceMessage).exceptionHandler(th->{
//                th.printStackTrace();
//            });//接收消息
//            vertx.eventBus().consumer(config.getString("consumer_messageCallBack"),messageHandler::onCallBackMsg);//qos1和2回调
//            vertx.eventBus().consumer(config.getString("consumer_getUserMsgAll"),messageHandler::onCallBackMsg);//获取用户离线消息
//            vertx.eventBus().consumer(config.getString("consumer_sendMessage"),messageHandler::onSendMessage).exceptionHandler(th->{
//                th.printStackTrace();
//            });//发送消息

            //注册mailclient
            MailClient mailClient =new MailClient();
            mailClient.mailConf(vertx);
            cn.orangeiot.message.handler.msg.MessageHandler msgHandler=new cn.orangeiot.message.handler.msg.MessageHandler(vertx,config);
            vertx.eventBus().consumer(UserAddr.class.getName()+SMS_CODE,msgHandler::SMSCode);
            vertx.eventBus().consumer(UserAddr.class.getName()+MAIL_CODE,msgHandler::mailCode);
        } else {
            // failed!
            logger.fatal(res.cause().getMessage(), res.cause());
        }
    }

}
