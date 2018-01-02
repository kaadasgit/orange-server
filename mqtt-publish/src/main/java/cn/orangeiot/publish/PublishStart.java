package cn.orangeiot.publish;

import cn.orangeiot.publish.verticle.publishVerticle;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2017-11-23
 */
public class PublishStart {


    private static Logger logger = LoggerFactory.getLogger(PublishStart.class);

    public static void main(String[] args){
        Vertx.vertx().deployVerticle(publishVerticle.class.getName(), rs->{
            if(rs.failed()){
                logger.error("deploy publishVerticle fail");
                rs.cause().printStackTrace();
            }else{
                logger.info("deploy publishVerticle successs");
            }
        });
    }
}
