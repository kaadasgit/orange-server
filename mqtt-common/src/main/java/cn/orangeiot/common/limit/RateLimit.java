package cn.orangeiot.common.limit;

import io.vertx.core.Vertx;

/**
 * @author zhang bo
 * @version 1.0
 * @Description 限流接口
 * @date 2018-08-27
 */
public interface RateLimit {

    void rustRequestAdd(String uid, Vertx vertx, String eventAddr);

    void rustResponseMinus(String uid, Vertx vertx, String eventAddr);
}
