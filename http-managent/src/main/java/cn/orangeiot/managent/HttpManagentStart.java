package cn.orangeiot.managent;

import cn.orangeiot.managent.verticle.HttpServerVerticle;
import io.vertx.core.Vertx;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.*;

import java.io.InputStream;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2017-12-08
 */
public class HttpManagentStart {

    private static Logger logger = LogManager.getLogger(HttpManagentStart.class);

    @SuppressWarnings("Duplicates")
    public static void main(String[] args) {
        //日志使用log4j2
        System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.Log4j2LogDelegateFactory");

        /**加载log4j2配置*/
        ConfigurationSource source = null;
        try {
            //加载log4j2配置
            InputStream in = HttpManagentStart.class.getResourceAsStream("/log4j2.xml");
            source = new ConfigurationSource(in);
            Configurator.initialize(null, source);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

        if (null != source) {
            Vertx.vertx().deployVerticle(HttpServerVerticle.class.getName(), rs -> {
                if (rs.failed()) {
                    logger.error("deploy HttpServerVerticle fail");
                } else {
                    logger.info("deploy HttpServerVerticle successs");
                }
            });
        }
    }
}
