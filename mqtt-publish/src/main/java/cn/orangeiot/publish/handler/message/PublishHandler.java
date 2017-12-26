package cn.orangeiot.publish.handler.message;

import cn.orangeiot.common.verify.VerifyParamsUtil;
import io.vertx.core.AsyncResult;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.Objects;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2017-12-11
 */
public class PublishHandler {

    private static Logger logger = LoggerFactory.getLogger(PublishHandler.class);


    private JsonObject jsonObject;

    private FuncHandler funcHandler;

    public PublishHandler(JsonObject jsonObject,FuncHandler funcHandler) {
        this.jsonObject = jsonObject;
        this.funcHandler= funcHandler;
    }

    /**
     * @Description 消息处理
     * @author zhang bo
     * @date 17-12-11
     * @version 1.0
     */
    public void onMessage(Message<JsonObject> message) {
        logger.info("==PublishHandler=onMessage==params -> " + message.body().toString());
        VerifyParamsUtil.verifyParams(message.body(), new JsonObject().put("userId", String.class.getName())
                .put("deviceId", String.class.getName()).put("gwId", String.class.getName()).put("topicName", String.class.getName())
                .put("clientId", String.class.getName()), (AsyncResult<JsonObject> rs) -> {
            if (rs.failed()) {
                //app业务
                if(Objects.nonNull(message.body().getValue("topicName")) && message.body().getString("topicName").equals(jsonObject.getString("app_fuc_message"))){
                    funcHandler.onMessage(message,(AsyncResult<JsonObject> returnData)->{
                        if(returnData.failed()){
                            message.reply(new JsonObject().put("code",404).put("msg",returnData.cause().getMessage())
                                    .put("topicName", jsonObject.getString("reply_message").replace("clientId",
                                    message.body().getString("uid"))));
                        }else{
                            message.reply(returnData.result().put("topicName", jsonObject.getString("reply_message").replace("clientId",
                                    message.body().getString("uid"))));
                        }
                    });
                }else{
                    message.reply(new JsonObject().put("code", 401));//参数校验失败
                }
            } else {
                String flag=rs.result().getString("clientId").split(":")[0];
                if(flag.equals("app")){//app 发送
                    message.reply(new JsonObject().put("flag", true).put("topicName", jsonObject.getString("repeat_message").replace("gwId",
                            message.body().getString("gwId"))));
                }else{//gw 发送
                    if(rs.result().getString("topicName").indexOf("/event")>=0) {//网关事件上报
                    }else{//reply回复app
                        message.reply(new JsonObject().put("flag", true).put("topicName", jsonObject.getString("reply_message").replace("clientId",
                                message.body().getString("userId"))));
                    }
                }
            }
        });
    }


}
