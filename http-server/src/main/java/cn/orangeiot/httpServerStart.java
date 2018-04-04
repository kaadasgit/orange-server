package cn.orangeiot;

import cn.orangeiot.http.verticle.HttpServerVerticle;
import io.vertx.core.Vertx;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Configurator;

import java.io.InputStream;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2017-12-08
 */
public class httpServerStart {
    private static Logger logger = LogManager.getLogger(httpServerStart.class);

    @SuppressWarnings("Duplicates")
    public static void main(String[] args) {
        //日志使用log4j2
        System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.Log4j2LogDelegateFactory");

        /**加载log4j2配置*/
        ConfigurationSource source = null;
        try {
            //加载log4j2配置
            InputStream in = httpServerStart.class.getResourceAsStream("/log4j2.xml");
            source = new ConfigurationSource(in);
            Configurator.initialize(null, source);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (null != source) {
            Vertx vertx = Vertx.vertx();
            vertx.deployVerticle(HttpServerVerticle.class.getName(), rs -> {
                if (rs.failed()) {
                    logger.error("deploy HttpServerVerticle fail");
                    rs.cause().printStackTrace();
                } else {
                    logger.info("deploy HttpServerVerticle successs");
                }
            });
        }
    }
}
