package cn.orangeiot;

import cn.orangeiot.topic.verticle.TopicVerticle;
import io.vertx.core.Vertx;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2017-11-23
 */
public class TopicStart {


    private static Logger logger = LogManager.getLogger(TopicStart.class);

    public static void main(String[] args){
        Vertx.vertx().deployVerticle(TopicVerticle.class.getName(), rs->{
            if(rs.failed()){
                logger.error("deploy TopicVerticle fail");
                rs.cause().printStackTrace();
            }else{
                logger.info("deploy TopicVerticle successs");
            }
        });
    }
}
