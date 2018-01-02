package cn.orangeiot;

import cn.orangeiot.message.verticle.MessageVerticle;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2017-11-23
 */
public class MessageStart {


    private static Logger logger = LoggerFactory.getLogger(MessageStart.class);

    public static void main(String[] args){
        Vertx.vertx().deployVerticle(MessageVerticle.class.getName(), rs->{
            if(rs.failed()){
                logger.error("deploy MessageVerticle fail");
                rs.cause().printStackTrace();
            }else{
                logger.info("deploy MessageVerticle successs");
            }
        });
    }
}
