package cn.orangeiot;

import cn.orangeiot.verticle.AuthVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2017-11-23
 */
public class Main {


    private static Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args){
        Vertx.vertx().deployVerticle(AuthVerticle.class.getName(),rs->{
            if(rs.failed()){
                logger.fatal("deploy authverticle fail");
                rs.cause().printStackTrace();
            }else{
                logger.info("deploy authverticle successs");
            }
        });
    }
}
