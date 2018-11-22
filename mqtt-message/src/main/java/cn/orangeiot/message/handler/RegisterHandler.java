package cn.orangeiot.message.handler;

import cn.orangeiot.message.handler.client.KafkaClient;
import cn.orangeiot.message.handler.client.MailClient;
import cn.orangeiot.message.handler.client.PushClient;
import cn.orangeiot.message.handler.client.SMSClient;
import cn.orangeiot.message.handler.msg.OffMessageHandler;
import cn.orangeiot.message.handler.notify.NotifyHandler;
import cn.orangeiot.message.handler.ota.OtaUpgradeHandler;
import cn.orangeiot.message.handler.push.MessagePushHandler;
import cn.orangeiot.message.verticle.MessageVerticle;
import cn.orangeiot.reg.EventbusAddr;
import cn.orangeiot.reg.message.MessageAddr;
import cn.orangeiot.reg.ota.OtaAddr;
import cn.orangeiot.reg.user.UserAddr;
import io.netty.util.CharsetUtil;
import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.io.FileInputStream;
import java.io.InputStream;

/**
 * @author zhang bo
 * @version 1.0 集群的handler事件注册
 * @Description
 * @date 2017-11-23
 */
public class RegisterHandler implements EventbusAddr {

    private static Logger logger = LogManager.getLogger(RegisterHandler.class);

    private JsonObject config;

    public RegisterHandler(JsonObject config) {
        this.config = config;
    }

    /**
     * @Description 注册事件
     * @author zhang bo
     * @date 17-11-23
     * @version 1.0
     */
    public void consumer(AsyncResult<Vertx> res) {
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
            MailClient mailClient = new MailClient();
            mailClient.mailConf(vertx);
            //注册smsclient
            SMSClient smsClient = new SMSClient();
            smsClient.smsConf(vertx);
            //推送客戶端
            PushClient pushClient = new PushClient();
            pushClient.loadAndroidConf(vertx);
            pushClient.loadIOSConf(vertx);
            pushClient.loadIOSVoipConf(vertx);
            //注册kafkaclient
//            KafkaClient kafkaClient=new KafkaClient();
//            kafkaClient.kafkaProducerConf(vertx);
//            kafkaClient.kafkaConsumerConf(vertx);

            //通知相關
            cn.orangeiot.message.handler.msg.MessageHandler msgHandler = new cn.orangeiot.message.handler.msg.MessageHandler(vertx, config);
            vertx.eventBus().consumer(UserAddr.class.getName() + SMS_CODE, msgHandler::SMSCode);
            vertx.eventBus().consumer(UserAddr.class.getName() + MAIL_CODE, msgHandler::mailCode);

            //通知相关
            NotifyHandler notifyHandler = new NotifyHandler(vertx, config);
            vertx.eventBus().consumer(MessageAddr.class.getName() + NOTIFY_GATEWAY_USER_ADMIN, notifyHandler::notifyGatewayAdmin);
            vertx.eventBus().consumer(MessageAddr.class.getName() + REPLY_GATEWAY_USER, notifyHandler::replyGatewayUser);

            //存储消息和处理消息
            OffMessageHandler offMessageHandler = new OffMessageHandler(vertx, config);
            vertx.eventBus().consumer(MessageAddr.class.getName() + SAVE_OFFLINE_MSG, offMessageHandler::productMsg);

            //ota升級處理
            OtaUpgradeHandler otaUpgradeHandler = new OtaUpgradeHandler(vertx, config);
            vertx.eventBus().consumer(OtaAddr.class.getName() + OTA_UPGRADE_PROCESS, otaUpgradeHandler::UpgradeProcess);


            //推送
            try {
                MessagePushHandler messagePushHandler = new MessagePushHandler(config, vertx);
                vertx.eventBus().consumer(MessageAddr.class.getName() + SEND_APPLICATION_NOTIFY, messagePushHandler::sendPushNotify);
                vertx.eventBus().consumer(MessageAddr.class.getName() + SEND_APPLICATION_SOUND_NOTIFY, messagePushHandler::sendPushSoundNotify);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        } else {
            // failed!
            logger.error(res.cause().getMessage(), res.cause());
        }
    }
}