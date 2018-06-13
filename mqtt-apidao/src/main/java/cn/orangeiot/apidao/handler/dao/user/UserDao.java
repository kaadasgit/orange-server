package cn.orangeiot.apidao.handler.dao.user;

import cn.orangeiot.apidao.client.MongoClient;
import cn.orangeiot.apidao.client.RedisClient;
import cn.orangeiot.apidao.conf.RedisKeyConf;
import cn.orangeiot.common.genera.ErrorType;
import cn.orangeiot.common.options.SendOptions;
import cn.orangeiot.common.utils.KdsCreateMD5;
import cn.orangeiot.common.utils.SHA1;
import cn.orangeiot.common.utils.UUIDUtils;
import cn.orangeiot.reg.memenet.MemenetAddr;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTOptions;
import io.vertx.ext.mongo.BulkOperation;
import io.vertx.ext.mongo.FindOptions;
import io.vertx.ext.mongo.UpdateOptions;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2017-12-07
 */
public class UserDao extends SynchUserDao implements MemenetAddr {

    private static Logger logger = LogManager.getLogger(UserDao.class);

    private JWTAuth jwtAuth;

    private Vertx vertx;

    public UserDao(JWTAuth jwtAuth, Vertx vertx) {
        this.jwtAuth = jwtAuth;
        this.vertx = vertx;
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
                String pwd = message.body().getString("password");

                if (Objects.nonNull(rs.result()) && pwd.equals(rs.result())) {
                    message.reply(true);
                } else {
                    if (flag == 3) {//app验证
                        message.reply(false);
                    } else {//网关
                        //查找DB
                        MongoClient.client.findOne("kdsUser", new JsonObject().put("userGwAccount", message.body().getString("username")), new JsonObject()
                                .put("userPwd", 1), res -> {
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
//    }Rge


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
        logger.info("params -> {}",message.body());
        //查找DB
        MongoClient.client.findOne("kdsUser", new JsonObject().put("userTel", message.body().getString("tel"))
                .put("versionType", message.body().getString("versionType")), new JsonObject()
                .put("userPwd", 1).put("pwdSalt", 1).put("_id", 1).put("nickName", 1)
                .put("meUsername", 1).put("mePwd", 1).put("userid", 1), res -> {
            if (res.failed()) {
                res.cause().printStackTrace();
            } else {
                logger.info("params -> ok");
                if (Objects.nonNull(res.result())) {
                    if (Objects.nonNull(res.result().getValue("pwdSalt"))) {//md5验证
                        encyPwd(res.result().put("username", message.body().getString("tel"))
                                .put("loginIP", message.body().getString("loginIP")), message, KdsCreateMD5.getMd5(KdsCreateMD5.getMd5(res.result().getString("pwdSalt") + message.body().getString("password"))));
                    } else {//sha1验证
                        encyPwd(res.result().put("username", message.body().getString("tel"))
                                .put("loginIP", message.body().getString("loginIP")), message, SHA1.encode(message.body().getString("password")));
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
        MongoClient.client.findOne("kdsUser", new JsonObject().put("userMail", message.body().getString("mail"))
                .put("versionType", message.body().getString("versionType")), new JsonObject()
                .put("userPwd", 1).put("pwdSalt", 1).put("_id", 1).put("nickName", 1)
                .put("meUsername", 1).put("mePwd", 1).put("userid", 1), res -> {
            if (res.failed()) {
                res.cause().printStackTrace();
            } else {
                if (Objects.nonNull(res.result())) {
                    if (Objects.nonNull(res.result().getValue("pwdSalt"))) {//md5验证
                        encyPwd(res.result().put("username", message.body().getString("mail"))
                                .put("loginIP", message.body().getString("loginIP")), message, KdsCreateMD5.getMd5(KdsCreateMD5.getMd5(res.result().getString("pwdSalt") + message.body().getString("password"))));
                    } else {//sha1验证
                        encyPwd(res.result().put("username", message.body().getString("mail"))
                                .put("loginIP", message.body().getString("loginIP")), message, SHA1.encode(message.body().getString("password")));
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
        RedisClient.client.get(message.body().getString("versionType") + ":" + message.body().getString("name"), rs -> {
            if (rs.failed()) {
                rs.cause().printStackTrace();
            } else {
                if (Objects.nonNull(rs.result()) && rs.result().equals(message.body().getString("tokens"))) {//验证码验证通过
                    MongoClient.client.findOne("kdsUser", new JsonObject().put(field, message.body().getString("name"))
                            .put("versionType", message.body().getString("versionType")), new JsonObject().put("_id", 1), as -> {//是否已经注册
                        if (as.failed()) {
                            as.cause().printStackTrace();
                        } else {
                            if (!Objects.nonNull(as.result())) {//没有注册
                                String password = SHA1.encode(message.body().getString("password"));
                                MongoClient.client.insert("kdsUser", new JsonObject().put(field, message.body().getString("name"))
                                        .put("userPwd", password).put("nickName", message.body().getString("name"))
                                        .put("versionType", message.body().getString("versionType"))
                                        .put("insertTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))), res -> {
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
            message.reply(new JsonObject().put("uid", jsonObject.getString("_id")).put("token", jwts[1])
                    .put("meUsername", jsonObject.getString("meUsername")).put("mePwd", jsonObject.getString("mePwd")));

            //更新登錄記錄
            String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            MongoClient.client.findOneAndUpdateWithOptions("kdsUserLog", new JsonObject().put("userName"
                    , jsonObject.getString("username")).put("versionType", message.body().getString("versionType"))
                    , new JsonObject().put("$set", new JsonObject().put("userName", jsonObject.getString("username"))
                            .put("loginTime", time).put("loginIp", jsonObject.getString("loginIP"))
                            .put("versionType", message.body().getString("versionType")))
                    , new FindOptions(), new UpdateOptions().setUpsert(true), logtime -> {
                        if (logtime.failed()) logtime.cause().printStackTrace();
                    });
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
        logger.info("==UserDao=getNickname==params->" + message.body().toString());
        RedisClient.client.hget(RedisKeyConf.USER_INFO, message.body().getString("uid"), ars -> {
            if (ars.failed()) {
                ars.cause().printStackTrace();
            } else {
                if (Objects.nonNull(ars.result())) {
                    message.reply(new JsonObject().put("nickName", new JsonObject(ars.result()).getString("nickName")));
                } else {
                    MongoClient.client.findOne("kdsUser", new JsonObject().put("_id", new JsonObject().put("$oid", message.body().getString("uid")))
                                    .put("versionType", message.body().getString("versionType")),
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
                                                onSynchUpdateUserInfo(new JsonObject().put("userPwd", KdsCreateMD5.getMd5(KdsCreateMD5.getMd5(
                                                        jsonObject.getString("pwdSalt") + message.body().getString("newpwd"))))
                                                        .put("uid", message.body().getString("uid")));
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
                                                onSynchUpdateUserInfo(new JsonObject().put("userPwd", SHA1.encode(message.body().getString("newpwd")))
                                                        .put("uid", message.body().getString("uid")));
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
        RedisClient.client.get(message.body().getString("versionType") + ":" + message.body().getString("name"), rs -> {
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
                    MongoClient.client.findOne("kdsUser", new JsonObject().put(field, message.body().getString("name"))
                                    .put("versionType", message.body().getString("versionType")),
                            new JsonObject().put("pwdSalt", "").put("_id", 1), mrs -> {
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
                                                        .put("versionType", message.body().getString("versionType"))
                                                , new JsonObject().put("$set", new JsonObject().put("userPwd", pwd)), res -> {
                                                    if (res.failed()) {
                                                        res.cause().printStackTrace();
                                                    } else if (res.succeeded()) {
                                                        message.reply(new JsonObject());
                                                    } else {
                                                        message.reply(null);
                                                    }
                                                });
                                        onSynchUpdateUserInfo(new JsonObject().put("userPwd", pwd).put("uid", mrs.result().getString("_id")));
                                    } else {
                                        message.reply(null, new DeliveryOptions().addHeader("code",
                                                String.valueOf(ErrorType.RESULT_CODE_FAIL.getKey())).addHeader("msg", ErrorType.RESULT_CODE_FAIL.getValue()));
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


    /**
     * @Description 米米网同步用户
     * @author zhang bo
     * @date 18-1-12
     * @version 1.0
     */
    public void meMeUser(Message<JsonObject> message) {
        //同步緩存
        RedisClient.client.hget(RedisKeyConf.USER_INFO, message.body().getString("uid"), rs -> {
            if (rs.failed()) {
                rs.cause().printStackTrace();
            } else {
                if (Objects.nonNull(rs.result()))
                    RedisClient.client.hset(RedisKeyConf.USER_INFO, message.body().getString("uid"),
                            new JsonObject(rs.result()).put("meUsername", message.body().getString("username"))
                                    .put("mePwd", message.body().getString("password"))
                                    .put("userid", message.body().getLong("userid")).toString(), as -> {
                                if (as.failed()) as.cause().printStackTrace();
                            });
            }
        });
        //同步db
        MongoClient.client.updateCollectionWithOptions("kdsUser", new JsonObject().put("_id", new JsonObject().put("$oid", message.body().getString("uid")))
                , new JsonObject().put("$set", new JsonObject().put("meUsername", message.body().getString("username"))
                        .put("mePwd", message.body().getString("password")).put("userid", message.body().getLong("userid")))
                , new UpdateOptions().setUpsert(true), rs -> {
                    if (rs.failed()) rs.cause().printStackTrace();
                });
    }


    /**
     * @Description 米米网用户批量注册
     * @author zhang bo
     * @date 18-1-26
     * @version 1.0
     */
    public void meMeUserBulk(Message<JsonObject> message) {
        MongoClient.client.count("kdsUser", new JsonObject().put("userid", new JsonObject().put("$exists", false))
                , rs -> {
                    if (rs.failed()) {
                        rs.cause().printStackTrace();
                        message.reply(null);
                    } else {
                        if (Objects.nonNull(rs.result()) && rs.result() > 0) {//存在未注册的用户
                            boolean flag = rs.result() % 100 == 0 ? true : false;//是否是倍数
                            Long num = rs.result() / 100;//次数
                            if (flag) {
                                bulkRequestRegister(num, 100L);
                                message.reply(new JsonObject());
                            } else {
                                bulkRequestRegister(num, 100L);
                                Long endTotal = rs.result() - 100 * num;//100倍数的余数
                                bulkRequestRegister(1L, endTotal);//餘下一次
                                message.reply(new JsonObject());
                            }
                        } else {
                            message.reply(new JsonObject());
                        }
                    }
                });
    }


    /**
     * @Description 批量请求注册 涕归
     * @author zhang bo
     * @date 18-1-27
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public boolean bulkRequestRegister(Long num, Long count) {
        if (num == 0) {
            return true;
        } else {
            Future.<List<JsonObject>>future(f -> MongoClient.client.findWithOptions("kdsUser", new JsonObject().put("userid"
                    , new JsonObject().put("$exists", false)), new FindOptions().setFields(new JsonObject().put("_id", 1))
                    .setLimit(count.intValue()), f))
                    .compose(users -> {//异步处理数据
                        if (Objects.nonNull(users) && users.size() > 0) {//用户id
                            List<BulkOperation> bulkOperations = new ArrayList<>();
                            JsonArray jsonArray = new JsonArray();//用户信息集合
                            for (int j = 0; j < count; j++) {//最大100次
                                String username = UUIDUtils.getUUID();
                                String password = UUIDUtils.getUUID();
                                jsonArray.add(new JsonObject().put("username", username).put("password", password));
                                //批量處理
                                JsonObject params = new JsonObject().put("type", BulkOperation.BulkOperationType.UPDATE)
                                        .put("filter", new JsonObject().put("_id", new JsonObject().put("$oid", users.get(j).getString("_id"))))
                                        .put("document", new JsonObject().put("$set",
                                                new JsonObject().put("meUsername", username).put("mePwd", password)))
                                        .put("upsert", true).put("multi", false);
                                bulkOperations.add(new BulkOperation(params));
                            }
                            return Future.<Map<String, Object>>future(f -> f.complete(new HashMap<String, Object>() {{
                                put("jsonList", jsonArray);
                                put("bulks", bulkOperations);
                            }}));
                        } else {
                            return Future.future(f -> f.fail("null data"));
                        }
                    }).compose(f ->//异步请求第三方接口
                    Future.<List<BulkOperation>>future(fu -> vertx.eventBus().send(MemenetAddr.class.getName() + REGISTER_USER_BULK, new JsonArray(f.get("jsonList").toString())
                            , (AsyncResult<Message<JsonObject>> as) -> {
                                if (as.failed()) {
                                    as.cause().printStackTrace();
                                    Future.future(e -> e.fail("request ===result ->  error"));
                                } else {
                                    JsonObject jsonObject = as.result().body().getJsonArray("results").getJsonObject(0);
                                    JsonArray results = as.result().body().getJsonArray("results");
                                    if (jsonObject.getInteger("result") == 0) {//成功
                                        logger.info("=========success============" + jsonObject.toString());
                                        List<BulkOperation> bulkOperations = ((List<BulkOperation>) f.get("bulks"));
                                        bulkOperations.forEach(e -> {
                                            Long userid = results.stream().filter(r -> new JsonObject(r.toString()).getString("username")
                                                    .equals(e.getDocument().getJsonObject("$set").getString("meUsername")))
                                                    .map(uid -> new JsonObject(uid.toString()).getLong("userid")).findFirst().orElse(null);
                                            if (Objects.nonNull(userid))
                                                e.getDocument().getJsonObject("$set").put("userid", userid);
                                        });
                                        fu.complete(bulkOperations);
                                    } else {
                                        logger.error("=========error============" + jsonObject.toString());
                                        fu.fail("request ===result ->  error");
                                    }
                                }
                            }))
            ).setHandler(f -> {//接口返回处理
                if (f.failed()) {
                    f.cause().printStackTrace();
                } else {
                    //修改用户数据
                    MongoClient.client.bulkWrite("kdsUser", f.result(), ars -> {
                        if (ars.failed()) {
                            ars.cause().printStackTrace();
                        } else {
                            logger.info("=========mongoBulk============" + JsonObject.mapFrom(ars.result()).toString());
                            bulkRequestRegister(num - 1, count);
                        }
                    });
                }
            });
        }
        return false;
    }


    /**
     * @Description 獲取網關管理員
     * @author zhang bo
     * @date 18-5-4
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void selectGWAdmin(Message<JsonObject> message) {
        MongoClient.client.findOne("kdsGatewayDeviceList",
                new JsonObject().put("deviceSN", message.body().getString("gwId")
                ).put("adminuid", new JsonObject().put("$exists", true)), new JsonObject()
                        .put("_id", 0).put("adminuid", 1), rs -> {
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


    /**
     * @Description 上報pushId
     * @author zhang bo
     * @date 18-5-4
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void uploadPushId(Message<JsonObject> message) {
        logger.info("==params -> " + message.body());
        RedisClient.client.hget(RedisKeyConf.USER_INFO, message.body().getString("uid"), rs -> {
            if (rs.failed()) {
                rs.cause().printStackTrace();
                message.reply(null);
            } else {
                if (Objects.nonNull(rs.result())) {
                    RedisClient.client.hset(RedisKeyConf.USER_INFO, message.body().getString("uid"),
                            new JsonObject(rs.result()).put("JPushId", message.body().getString("JPushId"))
                                    .put("type", message.body().getInteger("type")).toString(),
                            as -> {
                                if (as.failed()) {
                                    as.cause().printStackTrace();
                                    message.reply(null);
                                } else {
                                    message.reply(as.result());
                                }
                            });
                }
            }
        });
    }

    /**
     * @Description 获取PushId
     * @author zhang bo
     * @date 18-5-4
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void getPushId(Message<JsonObject> message) {
        logger.info("==params -> " + message.body());
        RedisClient.client.hget(RedisKeyConf.USER_INFO, message.body().getString("uid"), rs -> {
            if (rs.failed()) {
                rs.cause().printStackTrace();
                message.reply(null);
            } else {
                if (Objects.nonNull(rs.result()))
                    message.reply(new JsonObject(rs.result()));
                else
                    message.reply(null);
            }
        });
    }

}
