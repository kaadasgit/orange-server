package cn.orangeiot.event;

import cn.orangeiot.event.service.BaseService;
import cn.orangeiot.http.handler.BaseHandler;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

import java.io.Serializable;
import java.util.List;


/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-07-17
 */
public interface UserService extends Serializable {

    @userInterFace(url = "/test", requsetMethod = "get")
    void test(RoutingContext routingContext);

    @userInterFace(url = "/hello", requsetMethod = "get")
    void hello(RoutingContext routingContext);
}
