package cn.orangeiot.http.verticle;

import cn.orangeiot.http.spi.SpiConf;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2017-12-08
 */
public class HttpServerVerticle extends AbstractVerticle {


    private static Logger logger = LogManager.getLogger(HttpServerVerticle.class);


    @Override
    public void start(Future<Void> startFuture) throws Exception {
        SpiConf spiConf = new SpiConf(config());
        spiConf.loadClusting();//加载集群配置
        startFuture.complete();
    }


    @Override
    public void stop(Future<Void> stopFuture) throws Exception {
        super.stop(stopFuture);
    }

}
