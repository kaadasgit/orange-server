package cn.orangeiot;

import cn.orangeiot.verticle.TopicVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2017-11-23
 */
public class TopicStart {


    private static Logger logger = LoggerFactory.getLogger(TopicStart.class);

    public static void main(String[] args){
        Vertx.vertx().deployVerticle(TopicVerticle.class.getName(), rs->{
            if(rs.failed()){
                logger.fatal("deploy TopicVerticle fail");
                rs.cause().printStackTrace();
            }else{
                logger.info("deploy TopicVerticle successs");
            }
        });
    }
}
