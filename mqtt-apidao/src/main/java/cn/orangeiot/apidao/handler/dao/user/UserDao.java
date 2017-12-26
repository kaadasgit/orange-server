package cn.orangeiot.apidao.handler.dao.user;

import cn.orangeiot.apidao.client.MongoClient;
import cn.orangeiot.apidao.client.RedisClient;
import cn.orangeiot.apidao.conf.RedisKeyConf;
import cn.orangeiot.common.utils.KdsCreateMD5;
import cn.orangeiot.common.utils.SHA1;
import cn.orangeiot.common.utils.UUIDUtils;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.ArrayList;
import java.util.Objects;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2017-12-07
 */
public class UserDao extends SynchUserDao {

    private static Logger logger = LoggerFactory.getLogger(UserDao.class);

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
        RedisClient.client.hget(RedisKeyConf.USER_ACCOUNT, message.body(), rs -> {
            if (rs.failed()) {
                rs.cause().printStackTrace();
            } else {
                if (Objects.nonNull(rs.result()))
                    message.reply(true);
                else
                    message.reply(false);
            }
        });
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
                .put("userPwd", "").put("pwdSalt", "").put("_id", ""), res -> {
            if (res.failed()) {
                res.cause().printStackTrace();
            } else {
                if (Objects.nonNull(res.result())) {
                    if (Objects.nonNull(res.result().getValue("pwdSalt"))) {//md5验证
                        encyPwd(res.result(), message, KdsCreateMD5.getMd5(KdsCreateMD5.getMd5(res.result().getString("pwdSalt") + message.body().getString("password"))));
                    } else {//sha1验证
                        encyPwd(res.result(), message, SHA1.encode(message.body().getString("password")));
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
                .put("userPwd", "").put("pwdSalt", "").put("_id", ""), res -> {
            if (res.failed()) {
                res.cause().printStackTrace();
            } else {
                if (Objects.nonNull(res.result())) {
                    if (Objects.nonNull(res.result().getValue("pwdSalt"))) {//md5验证
                        encyPwd(res.result(), message, KdsCreateMD5.getMd5(KdsCreateMD5.getMd5(res.result().getString("pwdSalt") + message.body().getString("password"))));
                    } else {//sha1验证
                        encyPwd(res.result(), message, SHA1.encode(message.body().getString("password")));
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
                                        MongoClient.client.insert("kdsUser", new JsonObject().put(field, message.body().getString("name"))
                                                .put("userPwd", SHA1.encode(message.body().getString("password"))).put("nickName", message.body().getString("name")), res -> {
                                            if (res.failed()) {
                                                res.cause().printStackTrace();
                                            } else {
                                                String token = UUIDUtils.getUUID();
                                                String uid = res.result();
                                                //存储登录后token相关
                                                RedisClient.client.hset(RedisKeyConf.USER_ACCOUNT, token, uid
                                                        , ars -> {
                                                            if (ars.failed()) ars.cause().printStackTrace();
                                                        });
                                                message.reply(new JsonObject().put("token", token).put("uid", uid));
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
     * @Description 检验密码
     * @author zhang bo
     * @date 17-12-11
     * @version 1.0
     */
    public void encyPwd(JsonObject jsonObject, Message<JsonObject> message, String pwd) {
        if (pwd.equals(jsonObject.getString("userPwd"))) {
            String token = UUIDUtils.getUUID();
            RedisClient.client.hexists(RedisKeyConf.USER_ACCOUNT, jsonObject.getString("_id"), rrs -> {
                if (rrs.failed()) {
                    rrs.cause().printStackTrace();
                } else {
                    if (rrs.result() == 0) {//不存在
                        cacheUser(token, jsonObject.getString("_id"));
                    } else {
                        RedisClient.client.hget(RedisKeyConf.USER_ACCOUNT, jsonObject.getString("_id"), rs -> {
                            if (rs.failed()) {
                                rs.cause().printStackTrace();
                            } else {
                                RedisClient.client.hdel(RedisKeyConf.USER_ACCOUNT, rs.result(), as -> {
                                    if (as.failed()) as.cause().printStackTrace();
                                });
                                cacheUser(token, jsonObject.getString("_id"));
                            }
                        });
                    }
                }
            });
            message.reply(new JsonObject().put("uid", jsonObject.getString("_id")).put("token", token));
        } else {
            message.reply(null);
        }
    }


    /**
     * @Description 添加缓存
     * @author zhang bo
     * @date 17-12-14
     * @version 1.0
     */
    public void cacheUser(String token, String uid) {
        RedisClient.client.hset(RedisKeyConf.USER_ACCOUNT, token, uid
                , rs -> {
                    if (rs.failed()) rs.cause().printStackTrace();
                });
        RedisClient.client.hset(RedisKeyConf.USER_ACCOUNT, uid, token
                , rs -> {
                    if (rs.failed()) rs.cause().printStackTrace();
                });
    }

    /**
     * @Description 获取昵称
     * @author zhang bo
     * @date 17-12-13
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void getNickname(Message<JsonObject> message) {
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


    /**
     * @Description 修改用户昵称
     * @author zhang bo
     * @date 17-12-14
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void updateNickname(Message<JsonObject> message) {
        MongoClient.client.updateCollection("kdsUser", new JsonObject().put("_id", new JsonObject().put("$oid", message.body().getString("uid")))
                , new JsonObject().put("$set", new JsonObject().put("nickName", message.body().getString("nickname"))), rs -> {
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
    }


    /**
     * @Description 修改用户密码
     * @author zhang bo
     * @date 17-12-14
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void updateUserpwd(Message<JsonObject> message) {
        MongoClient.client.findOne("kdsUser", new JsonObject().put("_id", new JsonObject().put("$oid", message.body().getString("uid")))
                , new JsonObject().put("userPwd", "").put("_id", 0).put("pwdSalt", ""), as -> {
                    if (as.failed()) {
                        as.cause().printStackTrace();
                    } else {
                        if (Objects.nonNull(as.result())) {
                            if (Objects.nonNull(as.result().getValue("pwdSalt"))) {//MD5
                                if (as.result().getString("userPwd").equals(KdsCreateMD5.getMd5(KdsCreateMD5.getMd5(
                                        as.result().getString("pwdSalt") + message.body().getString("oldpwd")))))
                                    MongoClient.client.updateCollection("kdsUser", new JsonObject().put("_id", new JsonObject().put("$oid", message.body().getString("uid")))
                                            , new JsonObject().put("$set", new JsonObject().put("userPwd", KdsCreateMD5.getMd5(KdsCreateMD5.getMd5(
                                                    as.result().getString("pwdSalt") + message.body().getString("newpwd"))))), rs -> {
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
                                if (as.result().getString("userPwd").equals(SHA1.encode(message.body().getString("oldpwd"))))
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
        RedisClient.client.hget(RedisKeyConf.USER_ACCOUNT, message.body().getString("token"), rs -> {
            if (rs.failed()) {
                rs.cause().printStackTrace();
            } else {
                if (Objects.nonNull(rs.result())) {
                    RedisClient.client.hdelMany(RedisKeyConf.USER_ACCOUNT, new ArrayList<String>() {{
                        add(message.body().getString("token"));
                        add(rs.result());
                    }}, as -> {
                        if (as.failed())
                            as.cause().printStackTrace();
                    });
                }
            }
        });
        message.reply(new JsonObject());
    }
}
