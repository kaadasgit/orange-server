package cn.orangeiot.message.handler.msg;

import cn.orangeiot.common.utils.KdsCreateRandom;
import cn.orangeiot.message.handler.client.MailClient;
import cn.orangeiot.message.sms.SmsSingleSender;
import cn.orangeiot.message.sms.SmsSingleSenderResult;
import cn.orangeiot.reg.message.MessageAddr;
import io.vertx.core.AsyncResult;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.mail.MailMessage;
import io.vertx.ext.mail.impl.Utils;

import java.util.ArrayList;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2017-12-12
 */
public class MessageHandler implements MessageAddr {

    private static Logger logger = LoggerFactory.getLogger(cn.orangeiot.message.handler.dao.message.MessageHandler.class);

    private Vertx vertx;

    private JsonObject config;

    public MessageHandler(Vertx vertx, JsonObject config) {
        this.config = config;
        this.vertx = vertx;
    }

    /**
     * @Description 发送短信验证码
     * @author zhang bo
     * @date 17-12-12
     * @version 1.0
     */
    public void SMSCode(Message<JsonObject> message) {
        vertx.eventBus().send(MessageAddr.class.getName() + GET_CODE_COUNT, new JsonObject().put("tel", message.body().getString("tel"))
                .put("count", config.getInteger("codeCount")), (AsyncResult<Message<Boolean>> ars) -> {//查找手机验证码次数上限
            if (ars.failed()) {
                ars.cause().printStackTrace();
            } else {
                if (ars.result().body()) {
                    message.reply(new JsonObject());
                    //请求tx短信验证码同步快不是异步请求代码
                    vertx.executeBlocking(rs->{
                        String tokens = KdsCreateRandom.createRandom(6);

                        vertx.eventBus().send(MessageAddr.class.getName() + SAVE_CODE, new JsonObject().put("tel", message.body().getString("tel"))
                                .put("verifyCode", tokens));//缓存验证码

                        //请根据实际 appid 和 appkey 进行开发，以下只作为演示 sdk 使用
                        int appid = config.getInteger("appid");
                        String appkey = config.getString("appkey");

                        int tmplcnId = config.getInteger("tmplcnId");
                        int tmpworldId = config.getInteger("tmpworldId");
                        try {
                            SmsSingleSender singleSender = new SmsSingleSender(appid, appkey);
                            SmsSingleSenderResult singleSenderResult;
                            String code = message.body().getString("code");
                            if (code.equals("86")) {//地区限制
                                ArrayList<String> params = new ArrayList<>();
                                params.add(tokens);
                                singleSenderResult = singleSender.sendWithParam(code, message.body().getString("tel"), tmplcnId, params, "", "", "");
                                logger.info("====MessageHandler=SMSCode==sendResult==return -> "+singleSenderResult);
                            } else {
                                ArrayList<String> params = new ArrayList<>();
                                params.add(tokens);

                                singleSenderResult = singleSender.sendWithParam(code, message.body().getString("tel"), tmpworldId, params, "", "", "");
                                logger.info("====MessageHandler=SMSCode==sendResult==return -> "+singleSenderResult);
                            }
                            rs.complete(singleSenderResult);
                        } catch (Exception e) {
                            rs.fail(e);
                        }
                    },asyncResult -> {
                        if(ars.failed()){
                            ars.cause().printStackTrace();
                        }
                    });
                } else {
                    message.reply(null);
                }
            }
        });


    }


    /**
     * @Description 发送邮箱验证码
     * @author zhang bo
     * @date 17-12-12
     * @version 1.0
     */
    public void mailCode(Message<JsonObject> message) {
        vertx.eventBus().send(MessageAddr.class.getName() + GET_CODE_COUNT, new JsonObject().put("tel", message.body().getString("mail"))
                .put("count", config.getInteger("codeCount")), (AsyncResult<Message<Boolean>> ars) -> {
            if (ars.failed()) {
                ars.cause().printStackTrace();
            } else {
                if (ars.result().body()) {
                    message.reply(new JsonObject());
                    String tokens = KdsCreateRandom.createRandom(6);
                    vertx.eventBus().send(MessageAddr.class.getName() + SAVE_CODE, new JsonObject().put("tel", message.body().getString("mail"))
                            .put("verifyCode", tokens));//缓存验证码
                    //TODO 邮箱通知
                    String text = config.getString("email_text").replaceFirst("verifyCode", tokens);
                    MailClient.client.sendMail(new MailMessage().setTo(message.body().getString("mail"))
                            .setFrom(config.getString("email_fromAddress"))
                            .setText(text)
                            .setSubject(config.getString("email_subject")), rs -> {
                        if (rs.failed())
                            rs.cause().printStackTrace();
                        else
                            logger.info(">>>>send email success emailAddr -> " + message.body().getString("mail"));
                    });
                } else {
                    message.reply(null);
                }
            }
        });
    }
}
