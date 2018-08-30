package cn.orangeiot.http.handler.message;

import cn.orangeiot.common.genera.ErrorType;
import cn.orangeiot.common.genera.Result;
import cn.orangeiot.common.options.SendOptions;
import cn.orangeiot.common.utils.DataType;
import cn.orangeiot.http.verify.VerifyParamsUtil;
import cn.orangeiot.reg.user.UserAddr;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.util.Arrays;
import java.util.Objects;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2017-12-12
 */
public class MessageHandler implements UserAddr {

    private static Logger logger = LogManager.getLogger(MessageHandler.class);

    private EventBus eventBus;

    private JsonObject config;

    public MessageHandler(EventBus eventBus, JsonObject config) {
        this.eventBus = eventBus;
        this.config = config;
    }


    /**
     * @Description 发送手机短信验证码
     * @author zhang bo
     * @date 17-12-12
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void sendSMS(RoutingContext routingContext) {
        //验证参数的合法性
        VerifyParamsUtil.verifyParams(routingContext, new JsonObject().put("tel", DataType.STRING)
                .put("code", DataType.STRING), asyncResult -> {
            if (asyncResult.failed()) {
                routingContext.fail(401);
            } else {
                if (StringUtils.isNumeric(asyncResult.result().getString("tel")))//检验是否是合法數字
                    eventBus.send(UserAddr.class.getName() + SMS_CODE, asyncResult.result(), SendOptions.getInstance()
                            , rs -> {
                                if (rs.failed()) {
                                    routingContext.fail(501);
                                } else {
                                    if (Objects.nonNull(rs.result().body())) {
                                        routingContext.response().end(JsonObject.mapFrom(new Result<String>()).toString());
                                    } else {
                                        routingContext.response().end(JsonObject.mapFrom(new Result<String>()
                                                .setErrorMessage(ErrorType.CODE_COUNT_FAIL.getKey(), ErrorType.CODE_COUNT_FAIL.getValue()))
                                                .toString());
                                    }
                                }
                            });
                else
                    routingContext.fail(401);

            }
        });
    }


    /**
     * @Description 发送邮箱验证码
     * @author zhang bo
     * @date 17-12-12
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void sendMail(RoutingContext routingContext) {
        //验证参数的合法性
        VerifyParamsUtil.verifyParams(routingContext, new JsonObject().put("mail", DataType.STRING)
                , asyncResult -> {
                    if (asyncResult.failed()) {
                        routingContext.fail(401);
                    } else {
                        String regex = "\\w+(\\.\\w)*@\\w+(\\.\\w{2,3}){1,3}";
                        if (asyncResult.result().getString("mail").matches(regex)) //检验是否是合法的邮箱地址
                            eventBus.send(UserAddr.class.getName() + MAIL_CODE, asyncResult.result(), SendOptions.getInstance()
                                    , rs -> {
                                        if (asyncResult.failed()) {
                                            routingContext.fail(501);
                                        } else {
                                            if (Objects.nonNull(rs.result().body())) {
                                                routingContext.response().end(JsonObject.mapFrom(new Result<String>()).toString());
                                            } else {
                                                routingContext.response().end(JsonObject.mapFrom(new Result<String>()
                                                        .setErrorMessage(ErrorType.CODE_COUNT_FAIL.getKey(), ErrorType.CODE_COUNT_FAIL.getValue()))
                                                        .toString());
                                            }
                                        }
                                    });
                        else
                            routingContext.fail(401);
                    }
                });
    }
}
