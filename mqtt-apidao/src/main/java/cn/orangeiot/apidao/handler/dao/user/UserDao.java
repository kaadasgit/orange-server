package cn.orangeiot.apidao.handler.dao.user;

import cn.orangeiot.apidao.client.MongoClient;
import cn.orangeiot.apidao.client.RedisClient;
import cn.orangeiot.apidao.conf.RedisKeyConf;
import cn.orangeiot.common.genera.ErrorType;
import cn.orangeiot.common.utils.KdsCreateMD5;
import cn.orangeiot.common.utils.SHA1;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTOptions;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2017-12-07
 */
public class UserDao extends SynchUserDao {

    private static Logger logger = LoggerFactory.getLogger(UserDao.class);


    private JWTAuth jwtAuth;

    public UserDao(JWTAuth jwtAuth) {
        this.jwtAuth = jwtAuth;
    }

    /**
     * @Description 获取用户
     * @author zhang bo
     * @date 17-12-7
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void getUser(Message<JsonObject> message) {
        logger.info("==UserDao=getUser" + message.body());
        //查找缓存
        RedisClient.client.hget(RedisKeyConf.USER_ACCOUNT, message.body().getString("username"), rs -> {
            if (rs.failed()) {
                rs.cause().printStackTrace();
                message.reply(false);
            } else {
                int flag = Objects.nonNull(message.body().getString("clientId")) ? message.body().getString("clientId").indexOf(":") : 0;//验证标识
                String pwd = flag == 2 ? SHA1.encode(message.body().getString("password")) : message.body().getString("password");

                if (Objects.nonNull(rs.result()) && pwd.equals(rs.result())) {
                    message.reply(true);
                } else {
                    if (flag == 3) {//app验证
                        message.reply(false);
                    } else {//网关
                        //查找DB
                        MongoClient.client.findOne("kdsUser", new JsonObject().put("userGwAccount", message.body().getString("username")), new JsonObject()
                                .put("userPwd", ""), res -> {
                            if (res.failed()) {
                                res.cause().printStackTrace();
                                message.reply(false);
                            } else {
                                if (Objects.nonNull(res.result()) && pwd.equals(res.result().getString("userPwd"))) {
                                    message.reply(true);
                                    onSynchUser(res.result().put("username", message.body().getString("username")));
                                } else {
                                    message.reply(false);
                                }
                            }
                        });
                    }
                }
            }
        });

    }


    /**
     * @Description 上版本mqtt connect auth
     * @author zhang bo
     * @date 17-12-7
     * @version 1.0
     */
//    @SuppressWarnings("Duplicates")
//    public void getUser(Message<JsonObject> message) {
//        RedisClient.client.hget("userAccount:", ((JsonObject)message.body()).getString("username"), (rs) -> {
//            if (rs.failed()) {
//                rs.cause().printStackTrace();
//            } else if (Objects.nonNull(rs.result()) && GUID.MD5(((JsonObject)message.body()).getString("password") + ((String)rs.result()).split("::")[0]).equals(((String)rs.result()).split("::")[1])) {
//                message.reply(true);
//            } else {
//                MongoClient.client.findOne("sys_user", (new JsonObject()).put("account", ((JsonObject)message.body()).getString("username")), (new JsonObject()).put("type", "").put("status", "").put("salt", "").put("password", ""), (res) -> {
//                    if (res.failed()) {
//                        res.cause().printStackTrace();
//                    } else if (Objects.nonNull(res.result()) && GUID.MD5(((JsonObject)message.body()).getString("password") + ((JsonObject)res.result()).getString("salt").toString()).equals(((JsonObject)res.result()).getString("password").toString())) {
//                        message.reply((new JsonObject()).put("password", ((JsonObject)res.result()).getString("password")).put("salt", ((JsonObject)res.result()).getString("salt")).put("username", ((JsonObject)message.body()).getString("username")).put("code", true));
//                    } else {
//                        message.reply((new JsonObject()).put("code", false));
//                    }
//
//                });
//            }
//        });
//    }


    /**
     * @Description 检验是否登录
     * @author zhang bo
     * @date 17-12-12
     * @version 1.0
     */
    public void verifyLogin(Message<String> message) {
        try {
            JsonObject jsonObject = new JsonObject(new String(Base64.decodeBase64(message.body())));
            String uid = jsonObject.getString("_id");
            if (null != uid) {
                RedisClient.client.hget(RedisKeyConf.USER_ACCOUNT, uid, res -> {
                    if (res.failed()) {
                        message.reply(false);
                    } else if (Objects.nonNull(res.result()) && res.result().toString().equals(message.body())) {
                        message.reply(true, new DeliveryOptions().addHeader("uid", uid));
                    } else {
                        message.reply(false);
                    }
                });
            }
        } catch (Exception e) {
            message.reply(false);
        }
    }


    /**
     * 用户手机登录
     *
     * @param message
     */
    @SuppressWarnings("Duplicates")
    public void telLogin(Message<JsonObject> message) {
        //查找DB
        MongoClient.client.findOne("kdsUser", new JsonObject().put("userTel", message.body().getString("tel")), new JsonObject()
                .put("userPwd", 1).put("pwdSalt", 1).put("_id", 1).put("nickName", 1), res -> {
            if (res.failed()) {
                res.cause().printStackTrace();
            } else {
                if (Objects.nonNull(res.result())) {
                    if (Objects.nonNull(res.result().getValue("pwdSalt"))) {//md5验证
                        encyPwd(res.result().put("username", message.body().getString("tel")), message, KdsCreateMD5.getMd5(KdsCreateMD5.getMd5(res.result().getString("pwdSalt") + message.body().getString("password"))));
                    } else {//sha1验证
                        encyPwd(res.result().put("username", message.body().getString("tel")), message, SHA1.encode(message.body().getString("password")));
                    }
                } else {//登陆失败
                    message.reply(null);
                }

            }
        });
    }


    /**
     * 用户email登录
     *
     * @param message
     */
    @SuppressWarnings("Duplicates")
    public void mailLogin(Message<JsonObject> message) {
        //查找DB
        MongoClient.client.findOne("kdsUser", new JsonObject().put("userMail", message.body().getString("mail")), new JsonObject()
                .put("userPwd", 1).put("pwdSalt", 1).put("_id", 1).put("nickName", 1), res -> {
            if (res.failed()) {
                res.cause().printStackTrace();
            } else {
                if (Objects.nonNull(res.result())) {
                    if (Objects.nonNull(res.result().getValue("pwdSalt"))) {//md5验证
                        encyPwd(res.result().put("username", message.body().getString("mail")), message, KdsCreateMD5.getMd5(KdsCreateMD5.getMd5(res.result().getString("pwdSalt") + message.body().getString("password"))));
                    } else {//sha1验证
                        encyPwd(res.result().put("username", message.body().getString("mail")), message, SHA1.encode(message.body().getString("password")));
                    }
                } else {//登陆失败
                    message.reply(null);
                }

            }
        });
    }


    /**
     * @Description 手机号注册
     * @author zhang bo
     * @date 17-12-12
     * @version 1.0
     */

    public void registerTel(Message<JsonObject> message) {
        register(message, "userTel");
    }


    /**
     * @Description email注册
     * @author zhang bo
     * @date 17-12-12
     * @version 1.0
     */
    public void registerMail(Message<JsonObject> message) {
        register(message, "userMail");
    }


    /**
     * @Description 通用注册
     * @author zhang bo
     * @date 17-12-12
     * @version 1.0
     */
    public void register(Message<JsonObject> message, String field) {
        RedisClient.client.get(message.body().getString("name"), rs -> {
            if (rs.failed()) {
                rs.cause().printStackTrace();
            } else {
                if (Objects.nonNull(rs.result()) && rs.result().equals(message.body().getString("tokens"))) {//验证码验证通过
                    MongoClient.client.findOne("kdsUser", new JsonObject().put(field, message.body().getString("name"))
                            , new JsonObject().put("_id", ""), as -> {//是否已经注册
                                if (as.failed()) {
                                    as.cause().printStackTrace();
                                } else {
                                    if (!Objects.nonNull(as.result())) {//没有注册
                                        String password = SHA1.encode(message.body().getString("password"));
                                        MongoClient.client.insert("kdsUser", new JsonObject().put(field, message.body().getString("name"))
                                                .put("userPwd", password).put("nickName", message.body().getString("name")), res -> {
                                            if (res.failed()) {
                                                res.cause().printStackTrace();
                                            } else {
                                                String uid = res.result();
                                                String jwtStr = jwtAuth.generateToken(new JsonObject().put("_id", uid).put("username", message.body().getString("name")),
                                                        new JWTOptions());//jwt加密
                                                String[] jwts = StringUtils.split(jwtStr, ".");
                                                RedisClient.client.hset(RedisKeyConf.USER_ACCOUNT, uid,
                                                        jwts[1], jwtrs -> {
                                                            if (jwtrs.failed()) jwtrs.cause().printStackTrace();
                                                        });
                                                message.reply(new JsonObject().put("token", jwts[1]).put("uid", uid));
                                                onSynchUserInfo(new JsonObject().put("userPwd", password).put("nickName", message.body().getString("name"))
                                                        .put("_id", uid).put("username", message.body().getString("name")));
                                            }
                                        });
                                    } else {
                                        message.reply(null, new DeliveryOptions().addHeader("code",
                                                String.valueOf(ErrorType.REGISTER_USER_DICT_FAIL.getKey())).addHeader("msg", ErrorType.REGISTER_USER_DICT_FAIL.getValue()));

                                    }
                                }
                            });
                } else {
                    message.reply(null, new DeliveryOptions().addHeader("code",
                            String.valueOf(ErrorType.VERIFY_CODE_FAIL.getKey())).addHeader("msg", ErrorType.VERIFY_CODE_FAIL.getValue()));

                }
            }
        });
    }


    /**
     * @Description 检验密码
     * @author zhang bo
     * @date 17-12-11
     * @version 1.0
     */
    public void encyPwd(JsonObject jsonObject, Message<JsonObject> message, String pwd) {
        if (pwd.equals(jsonObject.getString("userPwd"))) {
            String jwtStr = jwtAuth.generateToken(new JsonObject().put("_id", jsonObject.getString("_id"))
                    .put("username", jsonObject.getString("username")), new JWTOptions());//jwt加密
            String[] jwts = StringUtils.split(jwtStr, ".");
            RedisClient.client.hset(RedisKeyConf.USER_ACCOUNT, jsonObject.getString("_id"),
                    jwts[1], rs -> {
                        if (rs.failed()) rs.cause().printStackTrace();
                    });
            message.reply(new JsonObject().put("uid", jsonObject.getString("_id")).put("token", jwts[1]));
            if (Objects.nonNull(jsonObject.getValue("username"))) {//同步数据
                onSynchUserInfo(jsonObject);
            }
        } else {
            message.reply(null);
        }

    }


    /**
     * @Description 获取昵称
     * @author zhang bo
     * @date 17-12-13
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void getNickname(Message<JsonObject> message) {
        logger.info("==UserDao=getNickname==params->"+message.body().toString());
        RedisClient.client.hget(RedisKeyConf.USER_INFO, message.body().getString("uid"), ars -> {
            if (ars.failed()) {
                ars.cause().printStackTrace();
            } else {
                if (Objects.nonNull(ars.result())) {
                    message.reply(new JsonObject().put("nickName", new JsonObject(ars.result()).getString("nickName")));
                } else {
                    MongoClient.client.findOne("kdsUser", new JsonObject().put("_id", new JsonObject().put("$oid", message.body().getString("uid"))),
                            new JsonObject().put("nickName", "").put("_id", 0), rs -> {
                                if (rs.failed()) {
                                    rs.cause().printStackTrace();
                                } else {
                                    if (Objects.nonNull(rs.result())) {
                                        message.reply(rs.result());
                                    } else {
                                        message.reply(null);
                                    }
                                }
                            });
                }
            }
        });
    }


    /**
     * @Description 修改用户昵称
     * @author zhang bo
     * @date 17-12-14
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void updateNickname(Message<JsonObject> message) {
        RedisClient.client.hget(RedisKeyConf.USER_INFO, message.body().getString("uid"), ars -> {
            if (ars.failed()) {
                ars.cause().printStackTrace();
            } else {
                if (Objects.nonNull(ars.result())) {
                    RedisClient.client.hset(RedisKeyConf.USER_INFO, message.body().getString("uid")
                            , new JsonObject(ars.result()).put("nickName", message.body().getString("nickname")).toString(), rs -> {
                                if (rs.failed()) {
                                    rs.cause().printStackTrace();
                                } else {
                                    message.reply(new JsonObject());
                                    //异步同步信息
                                    MongoClient.client.updateCollection("kdsUser", new JsonObject().put("_id", new JsonObject().put("$oid", message.body().getString("uid")))
                                            , new JsonObject().put("$set", new JsonObject().put("nickName", message.body().getString("nickname"))), mrs -> {
                                                if (mrs.failed()) {
                                                    mrs.cause().printStackTrace();
                                                } else {
                                                    if (Objects.nonNull(mrs.result()) && mrs.result().getDocModified() == 1) {
                                                        message.reply(new JsonObject());
                                                    } else {
                                                        message.reply(null);
                                                    }
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
     * @Description 修改用户密码
     * @author zhang bo
     * @date 17-12-14
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void updateUserpwd(Message<JsonObject> message) {
        RedisClient.client.hget(RedisKeyConf.USER_INFO, message.body().getString("uid"), as -> {
            if (as.failed()) {
                as.cause().printStackTrace();
            } else {
                if (Objects.nonNull(as.result())) {
                    JsonObject jsonObject = new JsonObject(as.result());
                    if (Objects.nonNull(jsonObject.getValue("pwdSalt"))) {//MD5
                        if (jsonObject.getString("userPwd").equals(KdsCreateMD5.getMd5(KdsCreateMD5.getMd5(
                                jsonObject.getString("pwdSalt") + message.body().getString("oldpwd")))))
                            MongoClient.client.updateCollection("kdsUser", new JsonObject().put("_id", new JsonObject().put("$oid", message.body().getString("uid")))
                                    , new JsonObject().put("$set", new JsonObject().put("userPwd", KdsCreateMD5.getMd5(KdsCreateMD5.getMd5(
                                            jsonObject.getString("pwdSalt") + message.body().getString("newpwd"))))), rs -> {
                                        if (rs.failed()) {
                                            rs.cause().printStackTrace();
                                        } else {
                                            if (Objects.nonNull(rs.result()) && rs.result().getDocModified() == 1) {
                                                message.reply(new JsonObject());
                                            } else {
                                                message.reply(null);
                                            }
                                        }
                                    });
                        else
                            message.reply(null);
                    } else {//SHA-1
                        if (jsonObject.getString("userPwd").equals(SHA1.encode(message.body().getString("oldpwd"))))
                            MongoClient.client.updateCollection("kdsUser", new JsonObject().put("_id", new JsonObject().put("$oid", message.body().getString("uid")))
                                    , new JsonObject().put("$set", new JsonObject().put("userPwd", SHA1.encode(message.body().getString("newpwd")))), rs -> {
                                        if (rs.failed()) {
                                            rs.cause().printStackTrace();
                                        } else {
                                            if (Objects.nonNull(rs.result()) && rs.result().getDocModified() == 1) {
                                                message.reply(new JsonObject());
                                            } else {
                                                message.reply(null);
                                            }
                                        }
                                    });
                        else
                            message.reply(null);
                    }
                } else {
                    message.reply(null);
                }
            }
        });


    }

    /**
     * @Description 忘记密码
     * @author zhang bo
     * @date 17-12-14
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void forgetUserpwd(Message<JsonObject> message) {
        RedisClient.client.get(message.body().getString("name"), rs -> {
            if (rs.failed()) {
                rs.cause().printStackTrace();
            } else {
                if (Objects.nonNull(rs.result()) && rs.result().equals(message.body().getString("tokens"))) {//验证码验证通过
                    String field = "";
                    if (message.body().getInteger("type") == 1) {//手机
                        field = "userTel";
                    } else {//邮箱
                        field = "userMail";
                    }
                    String finalField = field;
                    MongoClient.client.findOne("kdsUser", new JsonObject().put(field, message.body().getString("name")),
                            new JsonObject().put("pwdSalt", "").put("_id", 0), mrs -> {
                                if (mrs.failed()) {
                                    mrs.cause().printStackTrace();
                                } else {
                                    if (Objects.nonNull(mrs.result())) {//账户是否存在
                                        String pwd = "";
                                        if (Objects.nonNull(mrs.result().getValue("pwdSalt"))) {//MD5
                                            pwd = KdsCreateMD5.getMd5(KdsCreateMD5.getMd5(
                                                    mrs.result().getString("pwdSalt") + message.body().getString("pwd")));
                                        } else {
                                            pwd = SHA1.encode(message.body().getString("pwd"));
                                        }
                                        //重置密码
                                        MongoClient.client.updateCollection("kdsUser", new JsonObject().put(finalField, message.body().getString("name"))
                                                , new JsonObject().put("$set", new JsonObject().put("userPwd", pwd)), res -> {
                                                    if (res.failed()) {
                                                        res.cause().printStackTrace();
                                                    } else if (res.succeeded()) {
                                                        message.reply(new JsonObject());
                                                    } else {
                                                        message.reply(null);
                                                    }
                                                });
                                    } else {
                                        message.reply(null);
                                    }

                                }
                            });
                } else {
                    message.reply(null);
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
    public void suggestMsg(Message<JsonObject> message) {
        MongoClient.client.insert("kdsSuggest", message.body(), rs -> {
            if (rs.failed()) {
                rs.cause().printStackTrace();
            } else {
                message.reply(new JsonObject());
            }
        });
    }


    /**
     * @Description 用户登出
     * @author zhang bo
     * @date 17-12-20
     * @version 1.0
     */
    public void logOut(Message<JsonObject> message) {
        String uid = new JsonObject(new String(Base64.decodeBase64(message.body().getString("token")))).getString("_id");
        RedisClient.client.hdel(RedisKeyConf.USER_ACCOUNT, uid, rs -> {
            if (rs.failed()) rs.cause().printStackTrace();
        });//清除token
        RedisClient.client.hdel(RedisKeyConf.USER_INFO, uid, rs -> {
            if (rs.failed()) rs.cause().printStackTrace();
        });//清除缓存打用户信息
        message.reply(new JsonObject());
    }
}
