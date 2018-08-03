package cn.orangeiot.mqtt.log;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.kafka.client.consumer.KafkaConsumer;
import io.vertx.kafka.client.producer.KafkaProducer;
import io.vertx.redis.RedisClient;
import io.vertx.redis.RedisOptions;
import org.apache.commons.io.IOUtils;
import org.apache.kafka.clients.KafkaClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;

/**
 * @author zhang bo
 * @version 1.0
 * @Description log 消息储存处理
 * @date 2018-07-25
 */
public class LogVerticle extends AbstractVerticle {


    private static Logger logger = LogManager.getLogger(LogVerticle.class);

    private static KafkaConsumer<String, String> consumer;//kafka consumer消费者

    private static KafkaProducer<String, String> producer;//kafka producer客户端

    private static RedisClient redisClient;//redis Client

    private static Vertx vertxInstance;

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        this.vertxInstance = vertx;
        init(vertxInstance);
        startFuture.complete();
    }

    public static KafkaConsumer<String, String> getConsumer() {
        return consumer;
    }

    public static KafkaProducer<String, String> getProducer() {
        return producer;
    }

    public static RedisClient getRedisClient() {
        return redisClient;
    }


    public static Vertx getvertxInstance() {
        return vertxInstance;
    }

    /**
     * @Description kafka  consumer配置
     * @author zhang bo
     * @date 18-3-26
     * @version 1.0
     */
    public void kafkaConsumerConf(Vertx vertx) {
        InputStream consumerkfkIn = LogVerticle.class.getResourceAsStream("/kafka_consumer-conf.properties");
        try {
            Properties kfkconfig = new Properties();
            kfkconfig.load(consumerkfkIn);
            consumer = KafkaConsumer.create(vertx, kfkconfig);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != consumerkfkIn)
                try {
                    consumerkfkIn.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
    }


    /**
     * @Description kafka producer config
     * @author zhang bo
     * @date 18-3-26
     * @version 1.0
     */
    public void kafkaProducerConf(Vertx vertx) {
        InputStream kafkaIn = LogVerticle.class.getResourceAsStream("/kafka_producer-conf.properties");
        try {
            Properties config = new Properties();
            config.load(kafkaIn);
            producer = KafkaProducer.create(vertx, config);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != kafkaIn)
                try {
                    kafkaIn.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
    }

    /**
     * @Description redis client config
     * @author zhang bo
     * @date 18-3-26
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void redisClientConf(Vertx vertx) {
        InputStream redisIn = LogVerticle.class.getResourceAsStream("/redis-conf.json");
        String redisConf = "";//jdbc连接配置
        try {
            redisConf = IOUtils.toString(redisIn, "UTF-8");//获取配置
            if (!redisConf.equals("")) {
                JsonObject json = new JsonObject(redisConf);

                RedisOptions redisOptions = new RedisOptions().setHost(json.getString("host"))
                        .setPort(json.getInteger("port"));

                if (Objects.nonNull(json.getValue("password")))
                    redisOptions.setAuth(json.getString("password"));
                redisClient = io.vertx.redis.RedisClient.create(vertx, redisOptions);//创建redisclient
            }
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        } finally {
            if (null != redisIn)
                try {
                    redisIn.close();
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
        }
    }


    /**
     * @Description 处理化log processing
     * @author zhang bo
     * @date 18-7-25
     * @version 1.0
     */
    public void init(Vertx vertx) {
        kafkaConsumerConf(vertx);
        kafkaProducerConf(vertx);
        redisClientConf(vertx);
        new Spi().registerEvent(vertx);
    }


}
