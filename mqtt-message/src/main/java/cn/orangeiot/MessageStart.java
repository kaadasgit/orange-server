package cn.orangeiot;

import cn.orangeiot.message.verticle.MessageVerticle;
import io.vertx.core.Vertx;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Configurator;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2017-11-23
 */
public class MessageStart {


    private static Logger logger = LogManager.getLogger(MessageStart.class);

    public static void main(String[] args) {
        //日志使用log4j2
        System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.Log4j2LogDelegateFactory");

        /**加载log4j2配置*/
        ConfigurationSource source = null;
        try {
            //加载log4j2配置
            InputStream in = MessageStart.class.getResourceAsStream("/log4j2.xml");
            source = new ConfigurationSource(in);
            Configurator.initialize(null, source);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            if (!Objects.nonNull(System.getProperty("pushDev"))){
                logger.fatal("-DpushDev  is not boolean type");
                System.exit(1);
            }
        } catch (Exception e) {
            System.exit(1);
            logger.fatal("-DpushDev  is null or failure");
        }

        if (null != source) {

            Vertx.vertx().deployVerticle(MessageVerticle.class.getName(), rs -> {
                if (rs.failed()) {
                    logger.error("deploy MessageVerticle fail");
                    rs.cause().printStackTrace();
                } else {
                    logger.info("deploy MessageVerticle successs");
                }
            });
        }
    }
}
