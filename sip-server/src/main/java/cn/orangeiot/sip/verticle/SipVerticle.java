package cn.orangeiot.sip.verticle;

import cn.orangeiot.sip.SipServerStart;
import cn.orangeiot.sip.SipVertxFactory;
import cn.orangeiot.sip.constant.SipOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.spi.cluster.zookeeper.ZookeeperClusterManager;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.sip.SipFactory;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-01-30
 */
public class SipVerticle extends AbstractVerticle {

    private JsonObject jsonObject;//配置數據

    private static Logger logger = LogManager.getLogger(SipVerticle.class);

    @Override
    public void start(Future<Void> startFuture) throws Exception {

        loadConf();
        //todo 建立sip实例
        SipVertxFactory.getInstance().createListeningPoint(SipOptions.UDP, vertx, jsonObject);
        startFuture.complete();
    }


    /**
     * @Description 加載配置
     * @author zhang bo
     * @date 17-12-8
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void loadConf() {
        InputStream configIn = SipVerticle.class.getResourceAsStream("/config.json");//全局配置
        String config = "";
        try {
            config = IOUtils.toString(configIn, "UTF-8");

            if (StringUtils.isNotBlank(config)) {
                jsonObject = new JsonObject(config);
            } else {
                logger.info("========load conf in file ========fail");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
