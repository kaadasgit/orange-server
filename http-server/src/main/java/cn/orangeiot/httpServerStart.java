package cn.orangeiot;

import cn.orangeiot.http.verticle.HttpServerVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2017-12-08
 */
public class httpServerStart {

    private static Logger logger = LoggerFactory.getLogger(httpServerStart.class);

    public static void main(String[] args){
        Vertx.vertx().deployVerticle(HttpServerVerticle.class.getName(), rs->{
            if(rs.failed()){
                logger.fatal("deploy HttpServerVerticle fail");
                rs.cause().printStackTrace();
            }else{
                logger.info("deploy HttpServerVerticle successs");
            }
        });
    }
}
