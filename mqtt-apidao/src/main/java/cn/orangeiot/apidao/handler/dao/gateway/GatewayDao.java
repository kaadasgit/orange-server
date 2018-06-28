package cn.orangeiot.apidao.handler.dao.gateway;

import cn.orangeiot.apidao.client.MongoClient;
import cn.orangeiot.apidao.client.RedisClient;
import cn.orangeiot.apidao.conf.RedisKeyConf;
import cn.orangeiot.apidao.handler.dao.file.FileDao;
import cn.orangeiot.common.genera.ErrorType;
import cn.orangeiot.common.options.SendOptions;
import cn.orangeiot.reg.memenet.MemenetAddr;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.FindOptions;
import io.vertx.ext.mongo.UpdateOptions;
import io.vertx.ext.mongo.WriteOption;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-01-04
 */
public class GatewayDao {
    private static Logger logger = LogManager.getLogger(GatewayDao.class);


    /**
     * @Description 用户绑定网关
     * @author zhang bo
     * @date 18-1-4
     * @version 1.0
     */
    public void onbindGatewayByUser(Message<JsonObject> message) {
        MongoClient.client.findOne("kdsGatewayDeviceList", new JsonObject().put("deviceSN", message.body().getString("devuuid"))
                .put("isAdmin", 1), new JsonObject().put("_id", 1).put("uid", 1).put("adminuid", 1).put("adminName", 1)
                .put("adminNickname", 1), as -> {
            if (as.failed()) {
                as.cause().printStackTrace();
                message.reply(null);
            } else {
                if (Objects.nonNull(as.result())) {//存在管理员
                    if (Objects.nonNull(as.result().getValue("uid")) && as.result().getString("uid")
                            .equals(message.body().getString("uid"))) {
                        message.reply(new JsonObject(), new DeliveryOptions().addHeader("code",
                                String.valueOf(ErrorType.HAVE_ADMIN_BY_GATEWAY.getKey())).addHeader("msg", ErrorType.HAVE_ADMIN_BY_GATEWAY.getValue()));
                        return;
                    }
                    message.reply(new JsonObject(), new DeliveryOptions().addHeader("code",
                            String.valueOf(ErrorType.NOTIFY_ADMIN_BY_GATEWAY.getKey())).addHeader("msg", ErrorType.NOTIFY_ADMIN_BY_GATEWAY.getValue()));
                    RedisClient.client.hget(RedisKeyConf.USER_INFO, message.body().getString("uid"), userResult -> {
                        if (userResult.failed()) {
                            userResult.cause().printStackTrace();
                            message.reply(null);
                        } else {
                            //todo 加入审批列表
                            if (Objects.nonNull(userResult.result())) {
                                JsonObject jsonObject = new JsonObject(userResult.result());
                                MongoClient.client.findOne("kdsApprovalList", new JsonObject().put("deviceSN", message.body().getString("devuuid"))
                                        .put("uid", message.body().getString("uid")).put("type", 1)
                                        .put("status", 1), new JsonObject().put("_id", 1), approvalResult -> {
                                    if (approvalResult.failed()) {
                                        approvalResult.cause().printStackTrace();
                                    } else {
                                        //处理是否是催促
                                        if (!Objects.nonNull(approvalResult.result())) {
                                            String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                                            MongoClient.client.save("kdsApprovalList", new JsonObject()
                                                    .put("deviceSN", message.body().getString("devuuid")).put("uid", jsonObject.getString("_id"))
                                                    .put("deviceNickName", message.body().getString("devuuid")).put("username", jsonObject.getString("username"))
                                                    .put("userNickname", jsonObject.getString("username")).put("approvaluid", as.result().getString("adminuid"))
                                                    .put("approvalName", as.result().getString("adminName")).put("approvalNickname", as.result().getString("adminNickname"))
                                                    .put("requestTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                                                    .put("type", 1).put("approvalTime", time).put("status", 1), rs -> {//status 1 有效
                                                if (rs.failed()) rs.cause().printStackTrace();
                                            });
                                        }
                                    }
                                });

                            }
                        }
                    });
                } else {//绑定网关(角色管理员)
                    RedisClient.client.hget(RedisKeyConf.USER_INFO, message.body().getString("uid"), userResult -> {
                        if (userResult.failed()) {
                            userResult.cause().printStackTrace();
                            message.reply(null);
                        } else {
                            if (Objects.nonNull(userResult.result())) {
                                JsonObject jsonObject = new JsonObject(userResult.result());
                                MongoClient.client.save("kdsGatewayDeviceList", new JsonObject()
                                        .put("deviceSN", message.body().getString("devuuid")).put("uid", jsonObject.getString("_id"))
                                        .put("deviceNickName", message.body().getString("devuuid")).put("username", jsonObject.getString("username"))
                                        .put("userNickname", jsonObject.getString("username")).put("adminuid", jsonObject.getString("_id"))
                                        .put("adminName", jsonObject.getString("username")).put("adminNickname", jsonObject.getString("username")).put("isAdmin", 1)
                                        .put("bindTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                                        .put("userid", jsonObject.getLong("userid")), rs -> {
                                    if (rs.failed())
                                        message.reply(null);
                                    else
                                        message.reply(new JsonObject());
                                });
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
     * @Description 获取网关管理员
     * @author zhang bo
     * @date 18-1-5
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void onGetGatewayAdmin(Message<JsonObject> message) {
        MongoClient.client.findOne("kdsGatewayDeviceList", new JsonObject().put("deviceSN", message.body().getString("devuuid"))
                .put("isAdmin", 1), new JsonObject().put("_id", 1).put("deviceNickName", 1).put("deviceSN", 1)
                .put("adminuid", 1), as -> {
            if (as.failed()) {
                as.cause().printStackTrace();
            } else {
                if (Objects.nonNull(as.result())) {
                    RedisClient.client.hget(RedisKeyConf.USER_INFO, message.body().getString("uid"), rs -> {
                        if (rs.failed()) {
                            as.cause().printStackTrace();
                        } else {
                            if (Objects.nonNull(rs.result()))
                                message.reply(as.result().put("requestNickName", new JsonObject(rs.result()).getString("nickName")));
                            else
                                message.reply(null);
                        }
                    });

                } else {
                    message.reply(null);
                }
            }
        });
    }


    /**
     * @Description 审批普通用户绑定网关
     * @author zhang bo
     * @date 18-1-8
     * @version 1.0
     */
    public void onApprovalBindGateway(Message<JsonObject> message) {
        MongoClient.client.updateCollection("kdsApprovalList", new JsonObject().put("deviceSN", message.body().getString("devuuid"))
                .put("uid", message.body().getString("requestuid")), new JsonObject().put("$set", new JsonObject()
                .put("type", message.body().getInteger("type")).put("approvalTime"
                        , LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))), rs -> {
            if (rs.failed()) {
                rs.cause().printStackTrace();
            } else {
                message.reply(new JsonObject().put("type", message.body().getInteger("type")));//审批状态
            }
        });
        if (message.body().getInteger("type") == 2) {//审批通过
            RedisClient.client.hmget(RedisKeyConf.USER_INFO, new ArrayList<String>() {{
                add(message.body().getString("requestuid"));
                add(message.body().getString("uid"));
            }}, as -> {
                if (as.failed()) {
                    as.cause().printStackTrace();
                } else {
                    JsonObject reqUser = new JsonObject(as.result().getList().get(0).toString());//请求用户
                    JsonObject adminUser = new JsonObject(as.result().getList().get(1).toString());//管理员用户
                    MongoClient.client.save("kdsGatewayDeviceList", new JsonObject()
                            .put("deviceSN", message.body().getString("devuuid")).put("uid", reqUser.getString("_id"))
                            .put("deviceNickName", message.body().getString("devuuid")).put("username", reqUser.getString("username"))
                            .put("userNickname", reqUser.getString("nickName")).put("adminuid", adminUser.getString("_id"))
                            .put("adminName", adminUser.getString("username")).put("adminNickname", adminUser.getString("nickName")).put("isAdmin", 2)
                            .put("bindTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                            .put("userid", reqUser.getLong("userid")), rs -> {
                        if (rs.failed()) rs.cause().printStackTrace();
                    });


                }
            });
        }
    }


    /**
     * @Description 获取网关绑定列表
     * @author zhang bo
     * @date 18-1-10
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void onGetGatewayBindList(Message<JsonObject> message) {
        MongoClient.client.findWithOptions("kdsGatewayDeviceList", new JsonObject().put("uid",
                message.body().getString("uid")), new FindOptions().setFields(new JsonObject().put("deviceSN", 1)
                .put("deviceNickName", 1).put("adminuid", 1)), rs -> {
            if (rs.failed()) {
                rs.cause().printStackTrace();
                message.reply(null);
            } else {
                List<JsonObject> resultList = rs.result().stream().map(e -> {
                    if (e.getString("adminuid").equals(message.body().getString("uid")))
                        e.put("isAdmin", 1);//管理员
                    else
                        e.put("isAdmin", 2);//普通用户
                    e.remove("adminuid");
                    return e;
                }).collect(Collectors.toList());
                message.reply(new JsonArray(resultList));
            }
        });
    }


    /**
     * @Description 获取审批列表
     * @author zhang bo
     * @date 18-1-10
     * @version 1.0
     */
    public void onApprovalList(Message<JsonObject> message) {
        MongoClient.client.findWithOptions("kdsApprovalList", new JsonObject().put("approvaluid",
                message.body().getString("uid")).put("type", 1).put("status", 1), new FindOptions().setFields(new JsonObject().put("deviceSN", 1)
                .put("deviceNickName", 1).put("username", 1).put("userNickname", 1)
                .put("uid", 1).put("requestTime", 1)), rs -> {
            if (rs.failed()) {
                rs.cause().printStackTrace();
                message.reply(null);
            } else {
                message.reply(new JsonArray(rs.result()));
            }
        });
    }

    /**
     * @Description 获取用户信息
     * @author zhang bo
     * @date 18-1-13
     * @version 1.0
     */
    public void onGetUserInfo(Message<JsonObject> message) {
        RedisClient.client.hget(RedisKeyConf.USER_INFO, message.body().getString("uid"), rs -> {
            if (rs.failed()) {
                rs.cause().printStackTrace();
                message.reply(null);
            } else {
                message.reply(new JsonObject(rs.result()));
            }
        });
    }


    /**
     * @Description 修改网关的域
     * @author zhang bo
     * @date 18-1-13
     * @version 1.0
     */
    public void onupdateGWDomain(Message<JsonObject> message) {
        MongoClient.client.updateCollectionWithOptions("kdsGatewayDeviceList", new JsonObject().put("deviceSN", message.body().getString("devicesn"))
                , new JsonObject().put("$set", new JsonObject().put("domain", message.body().getString("domain")))
                , new UpdateOptions().setUpsert(false), rs -> {
                    if (rs.failed()) rs.cause().printStackTrace();
                });

        if (Objects.nonNull(message.body().getValue("type"))) {//綁定網關失敗
            MongoClient.client.removeDocument("kdsGatewayDeviceList", new JsonObject()
                    .put("deviceSN", message.body().getString("devicesn")).put("userid", Long.parseLong(message.body().getString("userid"))), rs -> {
                if (rs.failed())
                    rs.cause().printStackTrace();
            });
        }
    }


    /**
     * @Description 解绑网关
     * @author zhang bo
     * @date 18-1-15
     * @version 1.0
     */
    public void onUnbindGateway(Message<JsonObject> message) {
        MongoClient.client.findOne("kdsGatewayDeviceList", new JsonObject().put("deviceSN", message.body().getString("devuuid"))
                .put("adminuid", message.body().getString("uid")), new JsonObject().put("_id", 1), rs -> {
            if (rs.failed()) {
                rs.cause().printStackTrace();
                message.reply(null);
            } else {
                if (Objects.nonNull(rs.result())) {//管理员解绑
                    message.reply(new JsonObject(), new DeliveryOptions().addHeader("mult", "true"));
                } else {
                    message.reply(new JsonObject(), new DeliveryOptions().addHeader("mult", "false"));
                }
            }
        });
    }


    /**
     * @Description 获取mimi网用户userid
     * @author zhang bo
     * @date 18-1-15
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void onGetUserIdList(Message<JsonObject> message) {
        if (message.body().getString("mult").equals("true")) {
            MongoClient.client.findWithOptions("kdsGatewayDeviceList", new JsonObject().put("deviceSN"
                    , message.body().getString("devuuid")), new FindOptions().setFields(new JsonObject()
                    .put("userid", 1).put("_id", 0)), rs -> {
                if (rs.failed()) {
                    rs.cause().printStackTrace();
                    message.reply(null);
                } else {
                    message.reply(new JsonArray(rs.result()));

                    MongoClient.client.removeDocuments("kdsGatewayDeviceList", new JsonObject().put("deviceSN"
                            , message.body().getString("devuuid")).put("adminuid", message.body().getString("uid"))
                            , as -> {
                                if (as.failed()) {
                                    as.cause().printStackTrace();
                                    message.reply(null);
                                } else {
                                    message.reply(new JsonObject(), new DeliveryOptions().addHeader("mult", "true"));
                                }
                            });
                    //未审批的列表失效
                    MongoClient.client.updateCollectionWithOptions("kdsApprovalList", new JsonObject().put("deviceSN"
                            , message.body().getString("devuuid")).put("type", 1)
                            , new JsonObject().put("$set", new JsonObject().put("status", 2)//status 改为失效状态
                                    .put("failureTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))))
                            , new UpdateOptions().setMulti(true).setUpsert(true), as -> {
                                if (as.failed())
                                    as.cause().printStackTrace();
                            });
                }
            });
        } else {
            MongoClient.client.findWithOptions("kdsGatewayDeviceList", new JsonObject().put("deviceSN"
                    , message.body().getString("devuuid")).put("uid", message.body().getString("uid"))
                    , new FindOptions().setFields(new JsonObject()
                            .put("userid", 1).put("_id", 0)), rs -> {
                        if (rs.failed()) {
                            rs.cause().printStackTrace();
                            message.reply(null);
                        } else {
                            message.reply(new JsonArray(rs.result()));

                            MongoClient.client.removeDocument("kdsGatewayDeviceList", new JsonObject().put("deviceSN"
                                    , message.body().getString("devuuid")).put("uid", message.body().getString("uid"))
                                    , as -> {
                                        if (as.failed()) {
                                            as.cause().printStackTrace();
                                            message.reply(null);
                                        } else {
                                            message.reply(new JsonObject(), new DeliveryOptions().addHeader("mult", "false"));
                                        }
                                    });
                        }
                    });
        }
    }


    /**
     * @Description 删除网关用户
     * @author zhang bo
     * @date 18-1-15
     * @version 1.0
     */
    public void onDelGatewayUser(Message<JsonObject> message) {
        MongoClient.client.findOne("kdsGatewayDeviceList", new JsonObject().put("deviceSN", message.body().getString("devuuid"))
                        .put("adminuid", message.body().getString("uid")).put("uid", message.body().getString("uid"))
                , new JsonObject().put("_id", 0).put("userid", 1), rs -> {
                    if (rs.failed()) {
                        rs.cause().printStackTrace();
                        message.reply(null);
                    } else {
                        if (Objects.nonNull(rs.result())) {//是否是设备管理员
                            MongoClient.client.removeDocument("kdsGatewayDeviceList",
                                    new JsonObject().put("_id", new JsonObject().put("$oid", message.body().getString("_id"))), as -> {
                                        if (as.failed()) {
                                            as.cause().printStackTrace();
                                            message.reply(null);
                                        } else {
                                            message.reply(rs.result());
                                        }
                                    });
                        } else {
                            message.reply(null);
                        }
                    }
                });
    }


    /**
     * @Description 获取网关普通用户
     * @author zhang bo
     * @date 18-1-15
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void onGetGatewayUserList(Message<JsonObject> message) {
        MongoClient.client.findOne("kdsGatewayDeviceList", new JsonObject().put("deviceSN", message.body().getString("devuuid"))
                .put("adminuid", message.body().getString("uid")), new JsonObject().put("_id", 0), rs -> {
            if (rs.failed()) {
                rs.cause().printStackTrace();
                message.reply(null);
            } else {
                if (Objects.nonNull(rs.result())) {
                    MongoClient.client.findWithOptions("kdsGatewayDeviceList", new JsonObject().put("deviceSN"
                            , message.body().getString("devuuid")), new FindOptions().setFields(new JsonObject()
                            .put("uid", 1).put("username", 1).put("userNickname", 1).put("_id", 1)), as -> {
                        if (as.failed()) {
                            as.cause().printStackTrace();
                            message.reply(null);
                        } else {
                            message.reply(new JsonArray(as.result().stream().filter(
                                    e -> !e.getString("uid").equals(message.body().getString("uid"))).collect(Collectors.toList())));
                        }
                    });
                } else {
                    message.reply(null);
                }
            }
        });

    }


    /**
     * @Description 获取网关管理员
     * @author zhang bo
     * @date 18-1-16
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void onGetGatewayAdminByuid(Message<JsonObject> message) {
        MongoClient.client.findOne("kdsGatewayDeviceList", new JsonObject().put("deviceSN",
                message.body().getString("gwId")).put("isAdmin", 1), new JsonObject().put("uid", 1)
                .put("_id", 0), rs -> {
            if (rs.failed()) {
                rs.cause().printStackTrace();
                message.reply(null);
            } else {
                if (Objects.nonNull(rs.result()))
                    message.reply(rs.result());
                else
                    message.reply(null);
            }
        });
    }


    /**
     * @Description 設備上線
     * @author zhang bo
     * @date 18-3-22
     * @version 1.0
     */
    public void deviceOnline(Message<JsonObject> message) {
        MongoClient.client.findOne("kdsGatewayDeviceList", new JsonObject().put("deviceSN",
                message.body().getString("clientId").split(":")[1]).put("deviceList.devid", new JsonObject()
                .put("$in", new JsonArray().add(message.body().getString("deviceId")))), new JsonObject().put("_id", 1), as -> {
            if (as.failed()) {
                as.cause().printStackTrace();
            } else {
                if (Objects.nonNull(as.result()))
                    MongoClient.client.updateCollectionWithOptions("kdsGatewayDeviceList", new JsonObject().put("deviceSN",
                            message.body().getString("clientId").split(":")[1]).put("deviceList.devid", new JsonObject()
                                    .put("$in", new JsonArray().add(message.body().getString("deviceId")))),
                            new JsonObject().put("$set", new JsonObject().put("deviceList.$.version"
                                    , message.body().getJsonObject("eventparams").getString("SW"))
                                    .put("deviceList.$.devtype", message.body().getString("eventtype"))
                                    .put("deviceList.$.mac"
                                            , message.body().getJsonObject("eventparams").getString("mac_addr"))
                                    .put("status", 1)
                                    .put("time", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))))//上線狀態
                            , new UpdateOptions().setUpsert(false), rs -> {
                                if (rs.failed()) {
                                    rs.cause().printStackTrace();
                                }
                            });
                else
                    MongoClient.client.updateCollectionWithOptions("kdsGatewayDeviceList", new JsonObject().put("deviceSN",
                            message.body().getString("clientId").split(":")[1]),
                            new JsonObject().put("$addToSet", new JsonObject().put("deviceList", new JsonObject()
                                    .put("devid", message.body().getString("deviceId"))
                                    .put("devtype", message.body().getString("eventtype"))
                                    .put("version", message.body().getJsonObject("eventparams").getString("SW"))
                                    .put("mac", message.body().getJsonObject("eventparams").getString("mac_addr"))
                                    .put("status", 1)//上線狀態
                                    .put("time", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                            )), new UpdateOptions().setUpsert(false), rs -> {
                                if (rs.failed()) {
                                    rs.cause().printStackTrace();
                                }
                            });

            }
        });
    }


    /**
     * @Description 設備下線
     * @author zhang bo
     * @date 18-3-22
     * @version 1.0
     */
    public void deviceOffline(Message<JsonObject> message) {
        MongoClient.client.updateCollectionWithOptions("kdsGatewayDeviceList", new JsonObject().put("deviceSN",
                message.body().getString("clientId").split(":")[1]).put("deviceList.devid", new JsonObject()
                        .put("$in", new JsonArray().add(message.body().getString("deviceId")))),
                new JsonObject().put("$set", new JsonObject().put("deviceList.$.status"
                        , 2).put("time", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))))//下线狀態
                , new UpdateOptions().setUpsert(false), rs -> {
                    if (rs.failed()) {
                        rs.cause().printStackTrace();
                    }
                });
    }


    /**
     * @Description 獲取設備列表
     * @author zhang bo
     * @date 18-3-22
     * @version 1.0
     */
    public void getDeviceList(Message<JsonObject> message) {
        MongoClient.client.findOne("kdsGatewayDeviceList", new JsonObject().put("deviceSN",
                message.body().getString("devuuid")), new JsonObject().put("deviceList", 1).put("_id", 0), rs -> {
            if (rs.failed()) {
                rs.cause().printStackTrace();
            } else {
                message.reply(rs.result());
            }
        });
    }

}