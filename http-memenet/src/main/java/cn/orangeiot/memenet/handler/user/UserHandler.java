package cn.orangeiot.memenet.handler.user;

import cn.orangeiot.common.options.SendOptions;
import cn.orangeiot.common.utils.KdsCreateRandom;
import cn.orangeiot.common.utils.SHA256;
import cn.orangeiot.memenet.client.HttpClient;
import cn.orangeiot.reg.user.UserAddr;
import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.codec.BodyCodec;

import java.util.Arrays;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2017-12-28
 */
public class UserHandler implements UserAddr {

    private static Logger logger = LogManager.getLogger(UserHandler.class);

    private JsonObject conf;
    private Vertx vertx;

    public UserHandler(JsonObject conf, Vertx vertx) {
        this.conf = conf;
        this.vertx = vertx;
    }

    /**
     * @Description MIMI用户注册
     * @author zhang bo
     * @date 17-12-28
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void onRegisterUser(Message<JsonObject> message) {
        logger.info("==UserHandler=onRegisterUser===params -> " + message.body());
        String random = KdsCreateRandom.createRandom(10);//获取随机数
        // sha256加密
        SHA256.getSHA256Str(conf.getString("sig").replace("RANDOM_VALUE", random), as -> {
            if (as.failed()) {
                logger.error(as.cause().getMessage(), as);
                logger.error("==UserHandler==onRegister Usersha256 encrypt is fail");
            } else {
                //注册用户请求
                HttpClient.client.post("/v1/accsvr/accregister")
                        .addQueryParam("partid", conf.getString("partid"))
                        .addQueryParam("appid", conf.getString("appId"))
                        .addQueryParam("random", random)
                        .as(BodyCodec.jsonObject())
                        .sendJsonObject(new JsonObject().put("username", message.body().getString("username"))
                                .put("password", message.body().getString("password")).put("sig", as.result()), rs -> {
                            if (rs.failed()) {
                                logger.error(rs.cause().getMessage(), rs.cause());
                                logger.error("==UserHandler=onRegisterUser===request /v1/accsvr/accregister timeout");
                            } else {
                                logger.info("==UserHandler=onRegisterUser===request /v1/accsvr/accregister result -> " + rs.result().body());
                                if (rs.result().body().getInteger("result") == 0)
                                    vertx.eventBus().send(UserAddr.class.getName() + MEME_USER, message.body()
                                            .put("userid", rs.result().body().getLong("userid")), SendOptions.getInstance());
                            }
                        });
            }
        });
    }

    /**
     * @Description MIMI用户密码修改
     * @author zhang bo
     * @date 17-12-28
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void onUpdatePwd(Message<JsonObject> message) {
        logger.info("==UserHandler=onUpdatePwd===params -> " + message.body());
        String random = KdsCreateRandom.createRandom(10);//获取随机数
        // sha256加密
        SHA256.getSHA256Str(conf.getString("sig").replace("RANDOM_VALUE", random), as -> {
            if (as.failed()) {
                logger.error(as.cause().getMessage(), as);
                logger.error("==UserHandler==onUpdatePwd Usersha256 encrypt is fail");
            } else {
                //修改用户密码请求
                HttpClient.client.post("/v1/accsvr/resetpassword")
                        .addQueryParam("partid", conf.getString("partid"))
                        .addQueryParam("appid", conf.getString("appId"))
                        .addQueryParam("random", random)
                        .sendJsonObject(new JsonObject().put("userid", message.body().getString("userid"))
                                .put("password", message.body().getString("password")).put("sig", as.result()), rs -> {
                            if (rs.failed()) {
                                logger.error(rs.cause().getMessage(), rs.cause());
                                logger.error("==UserHandler=onUpdatePwd===request /v1/accsvr/resetpassword timeout");
                            } else {
                                logger.info("==UserHandler=onUpdatePwd===request /v1/accsvr/resetpassword result -> " + rs.result().body());
                            }
                        });
            }
        });
    }


    /**
     * @Description 用户批量注册
     * @author zhang bo
     * @date 18-1-26
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void onRegisterUserBulk(Message<JsonArray> message) {
        logger.info("==UserHandler=onRegisterUserBulk===");
        String random = KdsCreateRandom.createRandom(10);//获取随机数
        // sha256加密
        SHA256.getSHA256Str(conf.getString("sig").replace("RANDOM_VALUE", random), as -> {
            if (as.failed()) {
                logger.error(as.cause().getMessage(), as.cause());
                logger.error("==UserHandler==onRegisterUserBulk Usersha256 encrypt is fail");
            } else {
                //批量注册
                JsonObject jsonObject = new JsonObject().put("failmode", 2)//失败一次全部回滚
                        .put("users", message.body()).put("sig", as.result());
                logger.info("====UserHandler=onRegisterUserBulk==params -> {}", jsonObject);
                if (message.body().size() > 0) {
                    HttpClient.client.post("/v1/accsvr/accsregister")
                            .addQueryParam("partid", conf.getString("partid"))
                            .addQueryParam("appid", conf.getString("appId"))
                            .addQueryParam("random", random)
                            .as(BodyCodec.jsonObject())
                            .sendJsonObject(jsonObject, rs -> {
                                if (rs.failed()) {
                                    logger.error(rs.cause().getMessage(), rs.cause());
                                    logger.error("==UserHandler=onRegisterUserBulk===request /v1/accsvr/accsregister timeout");
                                } else {
                                    logger.info("==UserHandler=onRegisterUserBulk===request /v1/accsvr/accsregister result -> " + rs.result().body());
                                    message.reply(rs.result().body());
                                }
                            });
                }
            }
        });
    }
}
