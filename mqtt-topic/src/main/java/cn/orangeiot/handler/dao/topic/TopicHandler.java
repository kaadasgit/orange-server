package cn.orangeiot.handler.dao.topic;


import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.Objects;

/**
 * @author zhang bo  mqtt连接验证
 * @version 1.0
 * @Description
 * @date 2017-11-23
 */
public class TopicHandler {

    private static Logger logger = LoggerFactory.getLogger(TopicHandler.class);

    private Vertx vertx;

    private JsonObject config;

    public TopicHandler(Vertx vertx, JsonObject config) {
        this.config=config;
        this.vertx=vertx;
    }

    /**
     * @Description 创建主题
     * @author zhang bo
     * @date 17-11-23
     * @version 1.0
     */
    public void onSaveMessage(Message message){
        if(Objects.nonNull(message.body())){
            logger.info("==TopicHandler=onSaveMessage=params:"+message.body());
            JsonObject jsonObject=new JsonObject(message.body().toString());

            vertx.eventBus().send(config.getString("send_saveTopic"),jsonObject,(AsyncResult<Message<Boolean>> rs)->{
                if(Objects.nonNull(rs.result().body())){
                    if(rs.result().body())
                        message.reply(new JsonObject().put("code", 200).put("msg","成功"));
                    else
                        message.reply(new JsonObject().put("code", 202).put("msg","创建主题失败"));
                }
            });
        }
    }


    /**
     * @Description 删除主题
     * @author zhang bo
     * @date 17-11-23
     * @version 1.0
     */
    public void onDelMessage(Message message){
        if(Objects.nonNull(message.body())){
            logger.info("==TopicHandler=onDelMessage=params:"+message.body());
            JsonObject jsonObject=new JsonObject(message.body().toString());

            vertx.eventBus().send(config.getString("send_delTopic"),jsonObject,(AsyncResult<Message<Boolean>> rs)->{
                if(Objects.nonNull(rs.result().body())){
                    if(rs.result().body())
                        message.reply(new JsonObject().put("code", 200).put("msg","成功"));
                    else
                        message.reply(new JsonObject().put("code", 202).put("msg","创建主题失败"));
                }
            });
        }
    }

}
