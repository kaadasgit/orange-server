package cn.orangeiot.message.handler.msg;

import cn.orangeiot.message.constant.MQTopicConf;
import cn.orangeiot.message.handler.client.KafkaClient;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.kafka.client.producer.KafkaProducerRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author zhang bo
 * @version 1.0
 * @Description 离线消息处理
 * @date 2018-03-26
 */
public class OffMessageHandler {

    private static Logger logger = LogManager.getLogger(OffMessageHandler.class);

    private Vertx vertx;

    private JsonObject config;

    public OffMessageHandler(Vertx vertx, JsonObject config) {
        this.config = config;
        this.vertx = vertx;
    }


    /**
     * @Description 生产离线消息
     * @author zhang bo
     * @date 18-3-26
     * @version 1.0
     */
    public void productMsg(Message<JsonObject> message) {
        logger.info("====MessageHandler=SMSCode==sendResult==params -> params = {}", message);
        // 生產離線消息
        KafkaProducerRecord<String, String> records =
                KafkaProducerRecord.create(MQTopicConf.OFF_MESSAGE + message.body().getString("clientid")
                        , null, message.body().toString(), 0);

        KafkaClient.producer.write(records, done -> {
            if (done.succeeded())
                logger.info("Message " + records.value() + " written on topic=" + done.result().getTopic() +
                        ", partition=" + done.result().getPartition() +
                        ", offset=" + done.result().getOffset());
            else
                done.cause().printStackTrace();

        });
    }


//    public static void main(String[] args) {
//        Vertx vertx=Vertx.vertx();
//        Properties properties=new Properties();
//        properties.put("bootstrap.servers","114.67.58.242:9092");
//        properties.put("key.serializer","org.apache.kafka.common.serialization.StringSerializer");
//        properties.put("value.serializer","org.apache.kafka.common.serialization.StringSerializer");
//        properties.put("acks","1");
//        KafkaProducer<String, String> producer = KafkaProducer.create(vertx
//                , properties);
//
//        KafkaProducerRecord<String, String> records =
//                KafkaProducerRecord.create("cc", "cc");
//
//        producer.write(records, done -> {
//            if (done.succeeded())
//                System.out.println("Message " + records.value() + " written on topic=" + done.result().getTopic() +
//                        ", partition=" + done.result().getPartition() +
//                        ", offset=" + done.result().getOffset());
//            else
//                done.cause().printStackTrace();
//        });
//    }
}
