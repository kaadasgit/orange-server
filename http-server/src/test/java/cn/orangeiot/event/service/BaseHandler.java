package cn.orangeiot.event.service;

import io.vertx.core.Handler;

import java.io.Serializable;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-07-18
 */
public interface BaseHandler<RoutingContext> extends Handler<RoutingContext>, Serializable {

    void handle(RoutingContext event);
}
