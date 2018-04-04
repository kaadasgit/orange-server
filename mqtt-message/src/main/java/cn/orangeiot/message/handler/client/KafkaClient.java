package cn.orangeiot.message.handler.client;

import io.vertx.core.Vertx;
import io.vertx.kafka.client.consumer.KafkaConsumer;
import io.vertx.kafka.client.producer.KafkaProducer;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-03-26
 */
public class KafkaClient {

    public static KafkaConsumer<String, String> consumer;//kafka消费者


    public static KafkaProducer<String, String> producer;//kafka producer客户端


    /**
     * @Description kafka  consumer配置
     * @author zhang bo
     * @date 18-3-26
     * @version 1.0
     */
    public void kafkaConsumerConf(Vertx vertx) {
        InputStream consumerkfkIn = KafkaClient.class.getResourceAsStream("/kafka_consumer-conf.properties");
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
        InputStream kafkaIn = KafkaClient.class.getResourceAsStream("/kafka_producer-conf.properties");
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
}
