package cn.orangeiot.http.handler.user;

import cn.orangeiot.common.genera.ErrorType;
import cn.orangeiot.common.genera.Result;
import cn.orangeiot.common.options.SendOptions;
import cn.orangeiot.common.utils.DataType;
import cn.orangeiot.common.utils.UUIDUtils;
import cn.orangeiot.http.verify.VerifyParamsUtil;
import cn.orangeiot.reg.EventbusAddr;
import cn.orangeiot.reg.memenet.MemenetAddr;
import cn.orangeiot.reg.message.MessageAddr;
import cn.orangeiot.reg.user.UserAddr;
import io.vertx.core.AsyncResult;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.util.Objects;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2017-12-08
 */
public class UserHandler implements EventbusAddr {

    private static Logger logger = LogManager.getLogger(UserHandler.class);


    private EventBus eventBus;

    private JsonObject config;

    public UserHandler(EventBus eventBus, JsonObject config) {
        this.eventBus = eventBus;
        this.config = config;
    }

    /**
     * @Description 用户手机号码登录
     * @author zhang bo
     * @date 17-12-8
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void getUserByTel(RoutingContext routingContext) {
        logger.info("==UserHandler=getUserByTel==params->" + routingContext.getBodyAsString());
        //验证参数的合法性
        VerifyParamsUtil.verifyParams(routingContext, new JsonObject().put("tel", DataType.STRING)
                .put("password", DataType.STRING), asyncResult -> {
            if (asyncResult.failed()) {
                routingContext.fail(401);
            } else {
                Result<JsonObject> result = new Result<>();
                eventBus.send(UserAddr.class.getName() + LOGIN_TEL, asyncResult.result()
                                .put("loginIP", routingContext.request().remoteAddress().toString()), SendOptions.getInstance()
                        , (AsyncResult<Message<JsonObject>> rs) -> {
                            if (rs.failed()) {
                                routingContext.fail(501);
                            } else {
                                if (Objects.nonNull(rs.result().body())) {
                                    logger.info("==UserHandler=getUserByTel==login success==params-> {}, IP -> {}", routingContext.getBodyAsString(),
                                            routingContext.request().remoteAddress());
                                    result.setData(rs.result().body());
                                    routingContext.response().end(JsonObject.mapFrom(result).toString());
                                } else {
                                    logger.info("==UserHandler=getUserByTel==login fail==params-> {}, IP -> {}", routingContext.getBodyAsString(),
                                            routingContext.request().remoteAddress());
                                    result.setErrorMessage(ErrorType.RESULT_LOGIN_FIAL.getKey(), ErrorType.RESULT_LOGIN_FIAL.getValue());
                                    routingContext.response().end(JsonObject.mapFrom(result).toString());
                                }
                            }
                        });
            }
        });

    }


    /**
     * @Description 用户邮箱登录
     * @author zhang bo
     * @date 17-12-8
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void getUserByEmail(RoutingContext routingContext) {
        logger.info("==UserHandler=getUserByEmail==params->" + routingContext.getBodyAsString());
        //验证参数的合法性
        VerifyParamsUtil.verifyParams(routingContext, new JsonObject().put("mail", DataType.STRING)
                .put("password", DataType.STRING), asyncResult -> {
            if (asyncResult.failed()) {
                routingContext.fail(401);
            } else {
                Result<JsonObject> result = new Result<>();
                eventBus.send(UserAddr.class.getName() + LOGIN_MAIL, asyncResult.result()
                                .put("loginIP", routingContext.request().remoteAddress().toString()), SendOptions.getInstance()
                        , (AsyncResult<Message<JsonObject>> rs) -> {
                            if (rs.failed()) {
                                routingContext.fail(501);
                            } else {
                                if (Objects.nonNull(rs.result().body())) {
                                    logger.info("==UserHandler=getUserByEmail==login success==params-> {}, IP -> {}", routingContext.getBodyAsString(),
                                            routingContext.request().remoteAddress());
                                    result.setData(rs.result().body());
                                    routingContext.response().end(JsonObject.mapFrom(result).toString());
                                } else {
                                    logger.info("==UserHandler=getUserByEmail==login fail==params-> {}, IP -> {}", routingContext.getBodyAsString(),
                                            routingContext.request().remoteAddress());
                                    result.setErrorMessage(ErrorType.RESULT_LOGIN_FIAL.getKey(), ErrorType.RESULT_LOGIN_FIAL.getValue());
                                    routingContext.response().end(JsonObject.mapFrom(result).toString());
                                }
                            }
                        });
            }
        });

    }


    /**
     * @Description 手机注册用户
     * @author zhang bo
     * @date 17-12-12
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void registerUserByTel(RoutingContext routingContext) {
        //验证参数的合法性
        VerifyParamsUtil.verifyParams(routingContext, new JsonObject().put("name", DataType.STRING)
                .put("password", DataType.STRING).put("tokens", DataType.STRING), (AsyncResult<JsonObject> asyncResult) -> {
            if (asyncResult.failed()) {
                routingContext.fail(401);
            } else {
                if (StringUtils.isNumeric(asyncResult.result().getString("name")))//检验是否是合法的邮箱地址
                    register(asyncResult.result(), UserAddr.class.getName() + REGISTER_USER_TEL, routingContext);
                else
                    routingContext.fail(401);
            }
        });
    }


    /**
     * @Description 邮箱注册用户
     * @author zhang bo
     * @date 17-12-12
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void registerUserByMail(RoutingContext routingContext) {
        //验证参数的合法性
        VerifyParamsUtil.verifyParams(routingContext, new JsonObject().put("name", DataType.STRING)
                .put("password", DataType.STRING).put("tokens", DataType.STRING), (AsyncResult<JsonObject> asyncResult) -> {
            if (asyncResult.failed()) {
                routingContext.fail(401);
            } else {
                String regex = "\\w+(\\.\\w)*@\\w+(\\.\\w{2,3}){1,3}";
                if (asyncResult.result().getString("name").matches(regex)) //检验是否是合法的邮箱地址
                    register(asyncResult.result(), UserAddr.class.getName() + REGISTER_USER_MAIL, routingContext);
                else
                    routingContext.fail(401);
            }
        });

    }


    /**
     * @Description 通用注册
     * @author zhang bo
     * @date 17-12-12
     * @version 1.0
     */
    public void register(JsonObject jsonObject, String addr, RoutingContext routingContext) {
        //验证参数的合法性
        String regex = "^(?![0-9]+$)(?![a-zA-Z]+$)[0-9A-Za-z]{6,16}$";//字符和数字 6-16
        Result<JsonObject> result = new Result<>();
        if (jsonObject.getString("password").matches(regex)) {
            eventBus.send(addr, jsonObject, SendOptions.getInstance(), (AsyncResult<Message<JsonObject>> rs) -> {
                if (rs.failed()) {
                    routingContext.fail(501);
                } else {
                    if (Objects.nonNull(rs.result().body())) {
                        String meUsername = UUIDUtils.getUUID();
                        String mePassword = UUIDUtils.getUUID();
                        result.setData(rs.result().body().put("meUsername", meUsername).put("mePassword", mePassword));
                        routingContext.response().end(JsonObject.mapFrom(result).toString());

                        eventBus.send(MemenetAddr.class.getName() + REGISTER_USER, new JsonObject().put("username", meUsername)
                                .put("password", mePassword).put("uid", rs.result().body().getString("uid")), SendOptions.getInstance());//第三方数据同步
                    } else {
                        if (!rs.result().headers().isEmpty())
                            routingContext.response().end(JsonObject.mapFrom(
                                    result.setErrorMessage(Integer.parseInt(rs.result().headers().get("code")), rs.result().headers().get("msg"))).toString());
                        else
                            routingContext.response().end(JsonObject.mapFrom(
                                    result.setErrorMessage(ErrorType.REGISTER_USER_FAIL.getKey(), ErrorType.REGISTER_USER_FAIL.getValue())).toString());
                    }
                }
            });
        } else {
            result.setErrorMessage(ErrorType.PASSWORD_FAIL.getKey(), ErrorType.PASSWORD_FAIL.getValue());
            routingContext.response().end(JsonObject.mapFrom(result).toString());
        }
    }


    /**
     * @Description 获取用户昵称
     * @author zhang bo
     * @date 17-12-13
     * @version 1.0
     */
    public void getNickName(RoutingContext routingContext) {
        logger.info("==UserHandler=getNickName==");
        //验证参数的合法性
        VerifyParamsUtil.verifyParams(routingContext, new JsonObject().put("uid", DataType.STRING)
                , asyncResult -> {
                    if (asyncResult.failed()) {
                        routingContext.fail(401);
                    } else {
                        eventBus.send(UserAddr.class.getName() + GET_USER_NICKNAME, asyncResult.result(), SendOptions.getInstance()
                                , (AsyncResult<Message<JsonObject>> rs) -> {
                                    if (rs.failed()) {
                                        routingContext.fail(501);
                                    } else {
                                        if (Objects.nonNull(rs.result().body())) {
                                            routingContext.response().end(JsonObject.mapFrom(new Result<JsonObject>().setData(rs.result().body())).toString());
                                        } else {
                                            routingContext.response().end(JsonObject.mapFrom(
                                                    new Result<String>().setErrorMessage(ErrorType.GET_NICKNAME_FAIL.getKey(), ErrorType.GET_NICKNAME_FAIL.getValue())).toString());
                                        }
                                    }
                                });
                    }
                });
    }


    /**
     * @Description 修改用户昵称
     * @author zhang bo
     * @date 17-12-13
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void updateNickName(RoutingContext routingContext) {
        //验证参数的合法性
        VerifyParamsUtil.verifyParams(routingContext, new JsonObject().put("uid", DataType.STRING)
                .put("nickname", DataType.STRING), asyncResult -> {
            if (asyncResult.failed()) {
                routingContext.fail(401);
            } else {
                eventBus.send(UserAddr.class.getName() + UPDATE_USER_NICKNAME, asyncResult.result(), SendOptions.getInstance()
                        , (AsyncResult<Message<JsonObject>> rs) -> {
                            if (rs.failed()) {
                                routingContext.fail(501);
                            } else {
                                if (Objects.nonNull(rs.result().body())) {
                                    routingContext.response().end(JsonObject.mapFrom(new Result<JsonObject>()).toString());
                                } else {
                                    routingContext.response().end(JsonObject.mapFrom(
                                            new Result<String>().setErrorMessage(ErrorType.UPDATE_NICKNAME_FAIL.getKey(), ErrorType.UPDATE_NICKNAME_FAIL.getValue())).toString());
                                }
                            }
                        });
            }
        });
    }

    /**
     * @Description 修改密码
     * @author zhang bo
     * @date 17-12-14
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void updateUserPwd(RoutingContext routingContext) {
        //验证参数的合法性
        VerifyParamsUtil.verifyParams(routingContext, new JsonObject().put("newpwd", DataType.STRING)
                .put("oldpwd", DataType.STRING).put("uid", DataType.STRING), asyncResult -> {
            if (asyncResult.failed()) {
                routingContext.fail(401);
            } else {
                String regex = "^(?![0-9]+$)(?![a-zA-Z]+$)[0-9A-Za-z]{6,16}$";//字符和数字 6-16
                if (asyncResult.result().getString("newpwd").matches(regex)) {
                    eventBus.send(UserAddr.class.getName() + UPDATE_USER_PWD, asyncResult.result(), SendOptions.getInstance()
                            , (AsyncResult<Message<JsonObject>> rs) -> {
                                if (rs.failed()) {
                                    routingContext.fail(501);
                                } else {
                                    if (Objects.nonNull(rs.result().body())) {
                                        routingContext.response().end(JsonObject.mapFrom(new Result<JsonObject>()).toString());
                                    } else {
                                        routingContext.response().end(JsonObject.mapFrom(
                                                new Result<String>().setErrorMessage(ErrorType.UPDATE_USER_PWD_FAIL.getKey(), ErrorType.UPDATE_USER_PWD_FAIL.getValue())).toString());
                                    }
                                }
                            });
                } else {
                    routingContext.response().end(JsonObject.mapFrom(
                            new Result<>().setErrorMessage(ErrorType.PASSWORD_FAIL.getKey(), ErrorType.PASSWORD_FAIL.getValue())).toString());
                }
            }
        });
    }


    /**
     * @Description 忘记密码  type 1 手机  2 邮箱
     * @author zhang bo
     * @date 17-12-14
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void forgetPwd(RoutingContext routingContext) {
        //验证参数的合法性
        VerifyParamsUtil.verifyParams(routingContext, new JsonObject().put("name", DataType.STRING)
                .put("pwd", DataType.STRING).put("type", DataType.INTEGER).put("tokens", DataType.STRING), asyncResult -> {
            if (asyncResult.failed()) {
                routingContext.fail(401);
            } else {
                String regex = "^(?![0-9]+$)(?![a-zA-Z]+$)[0-9A-Za-z]{6,16}$";//字符和数字 6-16
                if (asyncResult.result().getString("pwd").matches(regex)) {
                    eventBus.send(UserAddr.class.getName() + FORGET_USER_PWD, asyncResult.result(), SendOptions.getInstance()
                            , (AsyncResult<Message<JsonObject>> rs) -> {
                                if (rs.failed()) {
                                    routingContext.fail(501);
                                } else {
                                    if (Objects.nonNull(rs.result().body())) {
                                        routingContext.response().end(JsonObject.mapFrom(new Result<JsonObject>()).toString());
                                    } else {
                                        if (!rs.result().headers().isEmpty())
                                            routingContext.response().end(JsonObject.mapFrom(
                                                    new Result<String>().setErrorMessage(Integer.parseInt(rs.result().headers().get("code")), rs.result().headers().get("msg"))).toString());
                                        else
                                            routingContext.response().end(JsonObject.mapFrom(
                                                    new Result<String>().setErrorMessage(ErrorType.UPDATE_USER_PWD_FAIL.getKey(), ErrorType.UPDATE_USER_PWD_FAIL.getValue())).toString());
                                    }
                                }
                            });
                } else {
                    routingContext.response().end(JsonObject.mapFrom(
                            new Result<>().setErrorMessage(ErrorType.PASSWORD_FAIL.getKey(), ErrorType.PASSWORD_FAIL.getValue())).toString());
                }
            }
        });
    }


    /**
     * @Description 用户留言
     * @author zhang bo
     * @date 17-12-14
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void suggestMsg(RoutingContext routingContext) {
        //验证参数的合法性
        VerifyParamsUtil.verifyParams(routingContext, new JsonObject().put("suggest", DataType.STRING)
                .put("uid", DataType.STRING), asyncResult -> {
            if (asyncResult.failed()) {
                routingContext.fail(401);
            } else {
                if (asyncResult.result().getString("suggest").length() > 300) {
                    routingContext.response().end(JsonObject.mapFrom(
                            new Result<String>().setErrorMessage(ErrorType.CONTENT_LENGTH_INDEX.getKey(), ErrorType.CONTENT_LENGTH_INDEX.getValue())).toString());
                } else {
                    eventBus.send(UserAddr.class.getName() + SUGGEST_MSG, asyncResult.result(), SendOptions.getInstance()
                            , (AsyncResult<Message<JsonObject>> rs) -> {
                                if (rs.failed()) {
                                    routingContext.fail(501);
                                } else {
                                    if (Objects.nonNull(rs.result().body())) {
                                        routingContext.response().end(JsonObject.mapFrom(new Result<JsonObject>()).toString());
                                    } else {
                                        routingContext.response().end(JsonObject.mapFrom(
                                                new Result<String>().setErrorMessage(ErrorType.CONTENT_SUGGEST_FAIL.getKey(), ErrorType.CONTENT_SUGGEST_FAIL.getValue())).toString());
                                    }
                                }
                            });
                }
            }
        });
    }


    /**
     * @Description 用户登出
     * @author zhang bo
     * @date 17-12-14
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void logOut(RoutingContext routingContext) {
        String token = routingContext.request().getHeader("token");
        if (StringUtils.isNotBlank(token)) {
            eventBus.send(UserAddr.class.getName() + USER_LOGOUT, new JsonObject().put("token", token), SendOptions.getInstance()
                    , rs -> {
                        if (rs.failed()) {
                            routingContext.fail(501);
                        } else {
                            if (Objects.nonNull(rs.result().body())) {
                                routingContext.response().end(JsonObject.mapFrom(new Result<JsonObject>()).toString());
                            } else {
                                routingContext.response().end(JsonObject.mapFrom(
                                        new Result<String>().setErrorMessage(ErrorType.LOGOUT_FAIL.getKey(), ErrorType.LOGOUT_FAIL.getValue())).toString());
                            }
                        }
                    });
        }
    }


    /**
     * @Description 上報jpushId
     * @author zhang bo
     * @date 18-5-22
     * @version 1.0
     */
    public void uploadJPushId(RoutingContext routingContext) {
        //验证参数的合法性
        VerifyParamsUtil.verifyParams(routingContext, new JsonObject().put("uid", DataType.STRING)
                .put("JPushId", DataType.STRING).put("type", DataType.INTEGER), asyncResult -> {//type 1 ios 2 android
            if (asyncResult.failed()) {
                routingContext.fail(401);
            } else {
                eventBus.send(UserAddr.class.getName() + UPLOAD_JPUSHID, asyncResult.result()
                        , SendOptions.getInstance(), rs -> {
                            if (rs.failed()) {
                                routingContext.fail(501);
                            } else {
                                if (Objects.nonNull(rs.result().body())) {
                                    routingContext.response().end(JsonObject.mapFrom(new Result<JsonObject>()).toString());
                                } else {
                                    routingContext.response().end(JsonObject.mapFrom(
                                            new Result<String>().setErrorMessage(ErrorType.UPLOAD_PUSHID_FAIL.getKey(), ErrorType.UPLOAD_PUSHID_FAIL.getValue())).toString());
                                }
                            }
                        });
            }
        });
    }


    /**
     * @Description 推送消息
     * @author zhang bo
     * @date 18-5-22
     * @version 1.0
     */
    public void sendPushNotify(RoutingContext routingContext) {
        //验证参数的合法性
        VerifyParamsUtil.verifyParams(routingContext, new JsonObject().put("uid", DataType.STRING)
                .put("content", DataType.STRING), asyncResult -> {//type 1 ios 2 android
            if (asyncResult.failed()) {
                routingContext.fail(401);
            } else {
                routingContext.response().end(JsonObject.mapFrom(new Result<JsonObject>()).toString());
                eventBus.send(MessageAddr.class.getName() + SEND_APPLICATION_NOTIFY, asyncResult.result());
            }
        });
    }

}
