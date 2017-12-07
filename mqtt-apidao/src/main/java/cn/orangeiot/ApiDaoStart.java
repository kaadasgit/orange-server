package cn.orangeiot;

import cn.orangeiot.verticle.ApiDaoVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2017-11-23
 */
public class ApiDaoStart {


    private static Logger logger = LoggerFactory.getLogger(ApiDaoStart.class);

    public static void main(String[] args){
        Vertx.vertx().deployVerticle(ApiDaoVerticle.class.getName(), rs->{
            if(rs.failed()){
                logger.fatal("deploy CacheVerticle fail");
                rs.cause().printStackTrace();
            }else{
                logger.info("deploy CacheVerticle successs");
            }
        });
    }
}
