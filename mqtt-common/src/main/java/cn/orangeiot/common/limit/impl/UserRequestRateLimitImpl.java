package cn.orangeiot.common.limit.impl;

import cn.orangeiot.common.constant.HttpAttrType;
import cn.orangeiot.common.genera.ErrorType;
import cn.orangeiot.common.genera.Result;
import cn.orangeiot.common.limit.RateLimit;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

/**
 * @author zhang bo
 * @version 1.0
 * @Description 根據用戶請求限流
 * @date 2018-08-27
 */
public class UserRequestRateLimitImpl implements RateLimit {


    /**
     * @param uid       用戶id
     * @param vertx     Instance
     * @param eventAddr 事件地址
     * @Description 用戶請求 count ++
     * @author zhang bo
     * @date 18-8-27
     * @version 1.0
     */
    @Override
    public void rustRequestAdd(String uid, Vertx vertx, String eventAddr) {
        vertx.eventBus().send(eventAddr, uid);
    }


    /**
     * @param uid       用戶id
     * @param vertx     Instance
     * @param eventAddr 事件地址
     * @Description 用戶process count --
     * @author zhang bo
     * @date 18-8-27
     * @version 1.0
     */
    @Override
    public void rustResponseMinus(String uid, Vertx vertx, String eventAddr) {
        vertx.eventBus().send(eventAddr, uid);
    }
}
