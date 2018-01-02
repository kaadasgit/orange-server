package cn.orangeiot.memenet.handler.user;

import cn.orangeiot.common.utils.KdsCreateRandom;
import cn.orangeiot.common.utils.SHA256;
import cn.orangeiot.memenet.client.HttpClient;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2017-12-28
 */
public class UserHandler {

    private static Logger logger = LoggerFactory.getLogger(UserHandler.class);

    private JsonObject conf;

    public UserHandler(JsonObject conf) {
        this.conf = conf;
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
        //TODO sha256加密
        SHA256.getSHA256Str(conf.getString("sig").replace("RANDOM_VALUE", random), as -> {
            if (as.failed()) {
                as.cause().printStackTrace();
                logger.error("==UserHandler==onRegister Usersha256 encrypt is fail");
            } else {
                //注册用户请求
                HttpClient.client.post("/v1/accsvr/accregister")
                        .addQueryParam("partid", conf.getString("partid"))
                        .addQueryParam("appid", conf.getString("appId"))
                        .addQueryParam("random", random)
                        .sendJsonObject(new JsonObject().put("username", message.body().getString("username"))
                                .put("password", message.body().getString("password")).put("sig", as.result()), rs -> {
                            if (rs.failed()) {
                                rs.cause().printStackTrace();
                                logger.error("==UserHandler=onRegisterUser===request /v1/accsvr/accregister timeout");
                            } else {
                                logger.info("==UserHandler=onRegisterUser===request /v1/accsvr/accregister result -> " + rs.result().body());
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
    public void onUpdatePwd(Message<JsonObject> message){
        logger.info("==UserHandler=onUpdatePwd===params -> " + message.body());
        String random = KdsCreateRandom.createRandom(10);//获取随机数
        //TODO sha256加密
        SHA256.getSHA256Str(conf.getString("sig").replace("RANDOM_VALUE", random), as -> {
            if (as.failed()) {
                as.cause().printStackTrace();
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
                                rs.cause().printStackTrace();
                                logger.error("==UserHandler=onUpdatePwd===request /v1/accsvr/resetpassword timeout");
                            } else {
                                logger.info("==UserHandler=onUpdatePwd===request /v1/accsvr/resetpassword result -> " + rs.result().body());
                            }
                        });
            }
        });
    }

}
