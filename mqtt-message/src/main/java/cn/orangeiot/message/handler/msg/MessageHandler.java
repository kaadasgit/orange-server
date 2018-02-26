package cn.orangeiot.message.handler.msg;

import cn.orangeiot.common.options.SendOptions;
import cn.orangeiot.common.utils.KdsCreateRandom;
import cn.orangeiot.common.utils.SHA256;
import cn.orangeiot.message.handler.client.MailClient;
import cn.orangeiot.message.handler.client.SMSClient;
import cn.orangeiot.reg.message.MessageAddr;
import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mail.MailMessage;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2017-12-12
 */
public class MessageHandler implements MessageAddr {

    private static Logger logger = LogManager.getLogger(MessageHandler.class);

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
                .put("count", config.getInteger("codeCount")), SendOptions.getInstance(), (AsyncResult<Message<Boolean>> ars) -> {//查找手机验证码次数上限
            if (ars.failed()) {
                ars.cause().printStackTrace();
                message.reply(null);
            } else {
                if (ars.result().body()) {
                    message.reply(new JsonObject());
                    //请求tx短信验证码同步快不是异步请求代码
                    String tokens = KdsCreateRandom.createRandom(6);//验证码
                    vertx.eventBus().send(MessageAddr.class.getName() + SAVE_CODE, new JsonObject().put("tel", message.body().getString("tel"))
                            .put("verifyCode", tokens));//缓存验证码
                    //请求参数
                    String appid = config.getString("appid");//appiid
                    String appkey = config.getString("appkey");//appkey
                    String random = KdsCreateRandom.createRandom(10);//随机数
                    Long time = System.currentTimeMillis() / 1000;//时间戳
                    int tpl_id;
                    if (message.body().getString("code").equals("86")) {//地区限制
                        tpl_id = config.getInteger("tmplcnId");//国内模板id

                    } else {
                        tpl_id = config.getInteger("tmpworldId");//国外模板id
                    }
                    //sha256签名
                    SHA256.getSHA256Str("appkey=APPKEY&random=RANDOM&time=TIME&mobile=MOBILE"
                            .replace("APPKEY", appkey).replace("RANDOM"
                                    , random).replace("TIME", time.toString()).replace("MOBILE", message.body().getString("tel")), rs -> {
                        if (rs.failed()) {
                            rs.cause().printStackTrace();
                        } else {
                            JsonObject jsonObject = new JsonObject();
                            logger.info("====MessageHandler=SMSCode==sendResult==params ->tel= {},nationcode = {}", message.body().getString("tel")
                                    , message.body().getString("code"));
                            jsonObject.put("tel", new JsonObject().put("nationcode", message.body().getString("code")).put("mobile", message.body().getString("tel")))
                                    .put("tpl_id", tpl_id).put("params", new JsonArray().add(tokens))
                                    .put("sig", rs.result()).put("time", time).put("extend", "").put("ext", "");
                            SMSClient.client.post("/v5/tlssmssvr/sendsms")
                                    .addQueryParam("sdkappid", appid)
                                    .addQueryParam("random", random)
                                    .sendJsonObject(jsonObject, qrs -> {
                                        if (qrs.failed()) {
                                            qrs.cause().printStackTrace();
                                        } else {
                                            logger.info("====MessageHandler=SMSCode==sendResult==return -> " + qrs.result().body());
                                        }
                                    });
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
                .put("count", config.getInteger("codeCount")), SendOptions.getInstance(), (AsyncResult<Message<Boolean>> ars) -> {
            if (ars.failed()) {
                ars.cause().printStackTrace();
                message.reply(null);
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
