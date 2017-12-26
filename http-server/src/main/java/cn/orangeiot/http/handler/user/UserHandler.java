package cn.orangeiot.http.handler.user;

import cn.orangeiot.common.genera.ErrorType;
import cn.orangeiot.common.genera.Result;
import cn.orangeiot.http.verify.VerifyParamsUtil;
import cn.orangeiot.reg.user.UserAddr;
import io.vertx.core.AsyncResult;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;
import io.vertx.reactivex.ext.auth.User;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2017-12-08
 */
public class UserHandler implements UserAddr {

    private static Logger logger = LoggerFactory.getLogger(UserHandler.class);


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
        VerifyParamsUtil.verifyParams(routingContext, new JsonObject().put("tel", String.class.getName())
                .put("password", String.class.getName()), asyncResult -> {
            if (asyncResult.failed()) {
                routingContext.fail(401);
            } else {
                Result<JsonObject> result = new Result<>();
                eventBus.send(UserAddr.class.getName() + LOGIN_TEL, asyncResult.result(), (AsyncResult<Message<JsonObject>> rs) -> {
                    if (rs.failed()) {
                        routingContext.fail(501);
                    } else {
                        if (Objects.nonNull(rs.result().body())) {
                            result.setData(rs.result().body());
                            routingContext.response().end(JsonObject.mapFrom(result).toString());
                        } else {
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
        VerifyParamsUtil.verifyParams(routingContext, new JsonObject().put("mail", String.class.getName())
                .put("password", String.class.getName()), asyncResult -> {
            if (asyncResult.failed()) {
                routingContext.fail(401);
            } else {
                Result<JsonObject> result = new Result<>();
                eventBus.send(UserAddr.class.getName() + LOGIN_MAIL, asyncResult.result(), (AsyncResult<Message<JsonObject>> rs) -> {
                    if (rs.failed()) {
                        routingContext.fail(501);
                    } else {
                        if (Objects.nonNull(rs.result().body())) {
                            result.setData(rs.result().body());
                            routingContext.response().end(JsonObject.mapFrom(result).toString());
                        } else {
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
        register(routingContext, UserAddr.class.getName() + REGISTER_USER_TEL);

    }


    /**
     * @Description 邮箱注册用户
     * @author zhang bo
     * @date 17-12-12
     * @version 1.0
     */
    public void registerUserByMail(RoutingContext routingContext) {
        register(routingContext, UserAddr.class.getName() + REGISTER_USER_MAIL);
    }


    /**
     * @Description 通用注册
     * @author zhang bo
     * @date 17-12-12
     * @version 1.0
     */
    public void register(RoutingContext routingContext, String addr) {
        //验证参数的合法性
        VerifyParamsUtil.verifyParams(routingContext, new JsonObject().put("name", String.class.getName())
                .put("password", String.class.getName()).put("tokens", String.class.getName()),(AsyncResult<JsonObject> asyncResult) -> {
            if (asyncResult.failed()) {
                routingContext.fail(401);
            } else {
                String regex = "^(?![0-9]+$)(?![a-zA-Z]+$)[0-9A-Za-z]{6,15}$";//字符和数字 6-15
                Result<JsonObject> result = new Result<>();
                if(asyncResult.result().getString("password").matches(regex)){
                    eventBus.send(addr,asyncResult.result(), (AsyncResult<Message<JsonObject>> rs) -> {
                        if (rs.failed()) {
                            routingContext.fail(501);
                        } else {
                            if (Objects.nonNull(rs.result().body())) {
                                result.setData(rs.result().body());
                                routingContext.response().end(JsonObject.mapFrom(result).toString());
                            } else {
                                result.setErrorMessage(ErrorType.REGISTER_USER_FAIL.getKey(), ErrorType.REGISTER_USER_FAIL.getValue());
                                routingContext.response().end(JsonObject.mapFrom(result).toString());
                            }
                        }
                    });
                }else{
                    result.setErrorMessage(ErrorType.PASSWORD_FAIL.getKey(), ErrorType.PASSWORD_FAIL.getValue());
                    routingContext.response().end(JsonObject.mapFrom(result).toString());
                }
            }
        });
    }


    /**
     * @Description 获取用户昵称
     * @author zhang bo
     * @date 17-12-13
     * @version 1.0
     */
    public void getNickName(RoutingContext routingContext) {
        //验证参数的合法性
        VerifyParamsUtil.verifyParams(routingContext, new JsonObject().put("uid", String.class.getName())
                , asyncResult -> {
                    if (asyncResult.failed()) {
                        routingContext.fail(401);
                    } else {
                        eventBus.send(UserAddr.class.getName() + GET_USER_NICKNAME, asyncResult.result(), (AsyncResult<Message<JsonObject>> rs) -> {
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
        VerifyParamsUtil.verifyParams(routingContext, new JsonObject().put("uid", String.class.getName())
                .put("nickname", String.class.getName()), asyncResult -> {
            if (asyncResult.failed()) {
                routingContext.fail(401);
            } else {
                eventBus.send(UserAddr.class.getName() + UPDATE_USER_NICKNAME, asyncResult.result(), (AsyncResult<Message<JsonObject>> rs) -> {
                    if (rs.failed()) {
                        routingContext.fail(501);
                    } else {
                        if (Objects.nonNull(rs.result().body())) {
                            routingContext.response().end(JsonObject.mapFrom(new Result<JsonObject>()).toString());
                        } else {
                            routingContext.response().end(JsonObject.mapFrom(
                                    new Result<String>().setErrorMessage(ErrorType.RESULT_CODE_FAIL.getKey(), ErrorType.RESULT_CODE_FAIL.getValue())).toString());
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
        VerifyParamsUtil.verifyParams(routingContext, new JsonObject().put("newpwd", String.class.getName())
                .put("oldpwd", String.class.getName()).put("uid", String.class.getName()), asyncResult -> {
            if (asyncResult.failed()) {
                routingContext.fail(401);
            } else {
                eventBus.send(UserAddr.class.getName() + UPDATE_USER_PWD, asyncResult.result(), (AsyncResult<Message<JsonObject>> rs) -> {
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
        VerifyParamsUtil.verifyParams(routingContext, new JsonObject().put("name", String.class.getName())
                .put("pwd", String.class.getName()).put("type", Integer.class.getName()).put("tokens", String.class.getName()), asyncResult -> {
            if (asyncResult.failed()) {
                routingContext.fail(401);
            } else {
                eventBus.send(UserAddr.class.getName() + FORGET_USER_PWD, asyncResult.result(), (AsyncResult<Message<JsonObject>> rs) -> {
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
        VerifyParamsUtil.verifyParams(routingContext, new JsonObject().put("suggest", String.class.getName())
                .put("uid", String.class.getName()), asyncResult -> {
            if (asyncResult.failed()) {
                routingContext.fail(401);
            } else {
                if (asyncResult.result().getString("suggest").length() > 300) {
                    routingContext.response().end(JsonObject.mapFrom(
                            new Result<String>().setErrorMessage(ErrorType.CONTENT_LENGTH_INDEX.getKey(), ErrorType.CONTENT_LENGTH_INDEX.getValue())).toString());
                } else {
                    eventBus.send(UserAddr.class.getName() + SUGGEST_MSG, asyncResult.result(), (AsyncResult<Message<JsonObject>> rs) -> {
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
        String token=routingContext.request().getHeader("token");
        if(StringUtils.isNotBlank(token)){
            eventBus.send(UserAddr.class.getName()+USER_LOGOUT,new JsonObject().put("token",token),rs->{
                if(rs.failed()){
                    routingContext.fail(501);
                }else{
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

}
