package cn.orangeiot;

import cn.orangeiot.auth.verticle.AuthVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2017-11-23
 */
public class AuthStart {


    private static Logger logger = LoggerFactory.getLogger(AuthStart.class);

    public static void main(String[] args){
        Vertx.vertx().deployVerticle(AuthVerticle.class.getName(), rs->{
            if(rs.failed()){
                logger.fatal("deploy authverticle fail");
                rs.cause().printStackTrace();
            }else{
                logger.info("deploy authverticle successs");
            }
        });
    }
}
