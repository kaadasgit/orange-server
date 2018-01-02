package cn.orangeiot.memenet;

import cn.orangeiot.memenet.verticle.MemeNetVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2017-11-23
 */
public class MemeNetStart {


    private static Logger logger = LoggerFactory.getLogger(MemeNetStart.class);

    public static void main(String[] args){
        Vertx.vertx().deployVerticle(MemeNetVerticle.class.getName(), rs->{
            if(rs.failed()){
                logger.fatal("deploy MemeNetVerticle fail");
                rs.cause().printStackTrace();
            }else{
                logger.info("deploy MemeNetVerticle successs");
            }
        });
    }
}
