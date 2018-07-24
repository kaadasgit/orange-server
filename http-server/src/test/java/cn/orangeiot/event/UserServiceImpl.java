package cn.orangeiot.event;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-07-17
 */
public class UserServiceImpl implements UserService{

    @Override
    public void test(RoutingContext routingContext) {
        routingContext.response().end(new JsonObject().put("code", 200).put("msg", "ok").encodePrettily());
    }

    @Override
    public void hello(RoutingContext routingContext) {
        routingContext.response().end(new JsonObject().put("code", 200).put("msg", "hello world!").encodePrettily());
    }
}
