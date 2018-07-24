package cn.orangeiot.rtp;

import cn.orangeiot.rtp.verticle.RtpVerticle;
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
 * @date 2018-03-08
 */
public class RtpServerStart {
    private static Logger logger = LogManager.getLogger(RtpServerStart.class);

    public static void main(String[] args) {

        //日志使用log4j2
        System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.Log4j2LogDelegateFactory");

        /**加载log4j2配置*/
        ConfigurationSource source = null;
        try {
            //加载log4j2配置
            InputStream in = RtpServerStart.class.getResourceAsStream("/log4j2.xml");
            source = new ConfigurationSource(in);
            Configurator.initialize(null, source);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

        if (null != source) {
            Vertx.vertx().deployVerticle(RtpVerticle.class.getName(), rs -> {
                if (rs.failed()) {
                    logger.error("deploy RtpServerStart fail");
                } else {
                    logger.info("deploy RtpServerStart successs");
                }
            });
        }
    }
}
