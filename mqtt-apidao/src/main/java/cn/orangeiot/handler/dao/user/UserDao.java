package cn.orangeiot.handler.dao.user;

import cn.orangeiot.client.MongoClient;
import cn.orangeiot.client.RedisClient;
import cn.orangeiot.conf.RedisKeyConf;
import cn.orangeiot.handler.dao.topic.TopicDao;
import cn.orangeiot.utils.GUID;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.Objects;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2017-12-07
 */
public class UserDao extends SynchUserDao{

    private static Logger logger = LoggerFactory.getLogger(UserDao.class);

    /**
     * @Description 获取用户
     * @author zhang bo
     * @date 17-12-7
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void getUser(Message<JsonObject> message){
        logger.info("==UserDao=getUser"+message.body());
        //查找缓存
        RedisClient.client.hget(RedisKeyConf.userAccount,message.body().getString("username"), rs->{
            if(rs.failed()){
                rs.cause().printStackTrace();
                message.reply(false);
            }else{
                if(Objects.nonNull(rs.result()) && GUID.MD5(message.body().getString("password")+
                        rs.result().split("::")[0]).equals(rs.result().split("::")[1])){
                    message.reply(true);
                }else{
                    //查找DB
                    MongoClient.client.findOne("sys_user",new JsonObject().put("account", message.body().getString("username")), new JsonObject()
                            .put("type", "").put("status", "").put("salt", "").put("password", ""),res->{
                        if (res.failed()) {
                            res.cause().printStackTrace();
                            message.reply(false);
                        } else {
                            if(Objects.nonNull(res.result())
                                    && GUID.MD5(message.body().getString("password") + res.result().getString("salt").toString()).equals((res.result().getString("password").toString()))){
                                message.reply(true);
                                onSynchUser(res.result().put("username",message.body().getString("username")));//异步同步数据
                            }else{
                                message.reply(false);
                            }
                        }
                    });
                }
            }
        });

    }

}
