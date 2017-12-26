package cn.orangeiot.job;

import cn.orangeiot.job.verticle.JobVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2017-12-18
 */
public class JobStart {

    private static Logger logger = LoggerFactory.getLogger(JobStart.class);

    public static void main(String[] args){
        Vertx.vertx().deployVerticle(JobVerticle.class.getName(), rs->{
            if(rs.failed()){
                logger.fatal("deploy JobVerticle fail");
                rs.cause().printStackTrace();
            }else{
                logger.info("deploy JobVerticle successs");
            }
        });
    }
}
