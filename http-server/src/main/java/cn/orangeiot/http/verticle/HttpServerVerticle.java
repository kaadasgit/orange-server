package cn.orangeiot.http.verticle;

import cn.orangeiot.http.spi.SpiConf;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2017-12-08
 */
public class HttpServerVerticle extends AbstractVerticle {


    private static Logger logger = LoggerFactory.getLogger(HttpServerVerticle.class);


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
