package cn.orangeiot.publish.service;

import cn.orangeiot.reg.memenet.MemenetAddr;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-12-20
 */
public interface MemeNetService extends MemenetAddr{

     /**
      * @Description 注册米米网用户
      * @author zhang bo
      * @date 18-12-20
      * @version 1.0
      * @param jsonObject 数据
      */
     void registerMeme(JsonObject jsonObject,Handler<AsyncResult<JsonObject>> handler);

     /**
      * @Description 注册米米网并且绑定设备
      * @author zhang bo
      * @date 18-12-20
      * @version 1.0
      * @param jsonObject 数据
      */
     void registerMemeAndBind(JsonObject jsonObject,Handler<AsyncResult<JsonObject>> handler);


     /**
      * @Description 米米网 绑定设备
      * @author zhang bo
      * @date 18-12-20
      * @version 1.0
      * @param jsonObject 数据
      */
     void bindMeme(JsonObject jsonObject,Handler<AsyncResult<JsonObject>> handler);

}
