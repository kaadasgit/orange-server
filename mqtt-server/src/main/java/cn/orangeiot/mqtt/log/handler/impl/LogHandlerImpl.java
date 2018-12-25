package cn.orangeiot.mqtt.log.handler.impl;

import cn.orangeiot.common.utils.DataType;
import cn.orangeiot.common.verify.VerifyParamsUtil;
import cn.orangeiot.mqtt.log.LogVerticle;
import cn.orangeiot.mqtt.log.handler.LogHandler;
import cn.orangeiot.mqtt.log.model.RedisKey;
import cn.orangeiot.reg.message.MessageAddr;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.kafka.client.common.TopicPartition;
import io.vertx.kafka.client.consumer.KafkaConsumer;
import io.vertx.kafka.client.producer.KafkaProducer;
import io.vertx.kafka.client.producer.KafkaProducerRecord;
import io.vertx.redis.RedisClient;
import io.vertx.redis.op.ScanOptions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-07-25
 */
public class LogHandlerImpl implements LogHandler {

    private final long DEFAULT_EXPIRE = 7 * 24 * 3600;//存活时间

    private final String DEFAULT_GATEWAY_TOPIC = "/orangeiot/gwId/call";//網關主題

    private final String DEFAULT_USER_TOPIC = "/clientId/rpc/reply";//用戶主題

    private final int DEFAULT_PAGE_NUM = 20;//每次查询20条

    private static Logger logger = LogManager.getLogger(LogHandlerImpl.class);

    private Vertx vertx;

    private KafkaProducer<String, String> producer;

    private KafkaConsumer<String, String> consumer;

    private RedisClient redisClient;

    private long expire;

    private int page_num;

    private String gatewayTopic;

    private String userTopic;

    public LogHandlerImpl(Vertx vertx, KafkaProducer<String, String> producer, KafkaConsumer<String, String> consumer
            , RedisClient redisClient) {
        this.vertx = vertx;
        this.producer = producer;
        this.consumer = consumer;
        this.redisClient = redisClient;
        this.expire = DEFAULT_EXPIRE;
        this.page_num = DEFAULT_PAGE_NUM;
        this.gatewayTopic = DEFAULT_GATEWAY_TOPIC;
        this.userTopic = DEFAULT_USER_TOPIC;
    }

    public LogHandlerImpl() {
        this.vertx = LogVerticle.getvertxInstance();
        this.producer = LogVerticle.getProducer();
        this.consumer = LogVerticle.getConsumer();
        this.redisClient = LogVerticle.getRedisClient();
        this.expire = DEFAULT_EXPIRE;
        this.page_num = DEFAULT_PAGE_NUM;
        this.gatewayTopic = DEFAULT_GATEWAY_TOPIC;
        this.userTopic = DEFAULT_USER_TOPIC;
    }

    /**
     * @Description write log
     * @author zhang bo
     * @date 18-7-25
     * @version 1.0
     */
    @Override
    public void writeLog(String msg, int msgId, long timeId, String topic, int partition) {
        logger.debug("params  msg -> {} , msgId -> {} , timeId -> {} , topic -> {} , partition -> {}"
                , msg, msgId, timeId, topic, partition);
        KafkaProducerRecord<String, String> record = wrapLog(msg, topic, partition);
        //write block
        producer.write(record, rs -> {
            if (rs.failed()) {
                logger.error(rs.cause().getMessage(), rs);
            } else {
                //維護數據offset
                String val = rs.result().getOffset() + ":" + timeId + ":" + System.currentTimeMillis();
                redisClient.hset(RedisKey.LOG_RECODR + topic, String.valueOf(msgId), val, as -> {
                    if (as.failed())
                        logger.error(as.cause().getMessage(), as);
                    else
                        //设置存活时间
                        redisClient.expire(RedisKey.LOG_RECODR + topic, expire, time -> {
                            if (time.failed()) logger.error(time.cause().getMessage(), time);
                        });
                });
            }
        });
    }


    /**
     * @Description write log
     * @author zhang bo
     * @date 18-7-25
     * @version 1.0
     */
    public void writeLog(String msg, int msgId, long timeId, String topic, int partition, Handler<AsyncResult<Boolean>> handler) {
        logger.debug("params  msg -> {} , msgId -> {} , timeId -> {} , topic -> {} , partition -> {}"
                , msg, msgId, timeId, topic, partition);
        KafkaProducerRecord<String, String> record = wrapLog(msg, topic, partition);
        //write block
        producer.write(record, rs -> {
            if (rs.failed()) {
                handler.handle(Future.failedFuture("write log fail"));
            } else {
                //維護數據offset
                String val = rs.result().getOffset() + ":" + timeId + ":" + System.currentTimeMillis();
                redisClient.hset(RedisKey.LOG_RECODR + topic, msgId + "", val, as -> {
                    if (as.failed()) {
                        handler.handle(Future.failedFuture("write log offset fail"));
                    } else {
                        if (as.result() == 1) {
                            handler.handle(Future.succeededFuture(true));
                            //设置存活时间
                            redisClient.expire(RedisKey.LOG_RECODR + topic, expire, time -> {
                                if (time.failed()) logger.error(time.cause().getMessage(), time);
                            });
                        } else
                            handler.handle(Future.succeededFuture(false));
                    }
                });

            }
        });
    }


    /**
     * @Description 写入日志
     * @author zhang bo
     * @date 18-7-27
     * @version 1.0
     */
    @Override
    public void writeLog(Message<JsonObject> message) {
        VerifyParamsUtil.verifyParams(message.body(), new JsonObject().put("msg", DataType.JSONOBJECT).put("msgId", DataType.INTEGER)
                .put("timeId", DataType.LONG).put("topic", DataType.STRING), rs -> {
            if (rs.failed()) {
                message.fail(401, "params verify fail");
            } else {
                if (Objects.nonNull(rs.result().getValue("partition")) && rs.result().getValue("partition") instanceof Integer) {
                    writeLog(rs.result().getJsonObject("msg").toString(), rs.result().getInteger("msgId"), rs.result().getInteger("timeId")
                            , rs.result().getString("topic"), rs.result().getInteger("partition"), as -> message.reply(as.result()));
                } else {
                    writeLog(rs.result().getJsonObject("msg").toString(), rs.result().getInteger("msgId"), rs.result().getInteger("timeId")
                            , rs.result().getString("topic"), as -> message.reply(as.result()));
                }
            }
        });
    }


    /**
     * @param msg       文本消息
     * @param partition 分区
     * @param topic     主题
     * @Description 包装log
     * @author zhang bo
     * @date 18-7-25
     * @version 1.0
     */
    public KafkaProducerRecord<String, String> wrapLog(String msg, String topic, int partition) {
        Objects.requireNonNull(msg);
        Objects.requireNonNull(topic);
        Objects.requireNonNull(partition);
        logger.debug("params : msg -> {} , topic -> {} , paratition -> {}", msg, topic, partition);
        return KafkaProducerRecord.create(topic, null, msg, partition);
    }


    /**
     * @param msg    文本消息
     * @param msgId  消息MSG id
     * @param timeId re定时器 id
     * @param topic  主题
     * @Description 寫入消息
     * @author zhang bo
     * @date 18-7-25
     * @version 1.0
     */
    @Override
    public void writeLog(String msg, int msgId, long timeId, String topic) {
        writeLog(msg, msgId, timeId, topic, 0);
    }

    /**
     * @param msg     文本消息
     * @param msgId   消息MSG id
     * @param timeId  re定时器 id
     * @param topic   主题
     * @param handler return handler async
     * @Description 寫入消息
     * @author zhang bo
     * @date 18-7-25
     * @version 1.0
     */
    @Override
    public void writeLog(String msg, int msgId, long timeId, String topic, Handler<AsyncResult<Boolean>> handler) {
        writeLog(msg, msgId, timeId, topic, 0, handler);
    }


    /**
     * @Description 获取offset集合
     * @author zhang bo
     * @date 18-7-31
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public List<Long> getOffsets(JsonArray jsonArray, String topic) {
        JsonArray resJsonArray = jsonArray.getJsonArray(1);
        if (jsonArray.size() > 0) {
            JsonArray jsons = new JsonArray();
            for (int i = 1; i < resJsonArray.size(); i = i + 2) {
                String[] arr = resJsonArray.getString(i).split(":");
                if (Long.parseLong(arr[2]) / 1000 - System.currentTimeMillis() / 1000 < expire) {
                    jsons.add(arr[0]);
                } else {//惰性刪除
                    redisClient.hdel(RedisKey.LOG_RECODR + topic, resJsonArray.getString(i - 1), rs -> {
                        if (rs.failed())
                            logger.error(rs.cause().getMessage(), rs);
                    });
                }
            }
            return jsons.stream().map(e -> Long.parseLong(e.toString())).sorted().collect(Collectors.toList());
        } else {
            return new ArrayList<>(0);
        }
    }

    /**
     * @Description 推送消息
     * @author zhang bo
     * @date 18-7-31
     * @version 1.0
     */
    public void publishMsg(String val, String clientid) {
        JsonObject jsonObject = new JsonObject(val);
        String topic = "";
        if (jsonObject.getString("dst").indexOf("app") >= 0) {
            topic = userTopic.replace("clientId", clientid);
        } else {
            topic = gatewayTopic.replace("gwId", clientid);
        }
        vertx.eventBus().send(MessageAddr.class.getName() + SEND_STORAGE_MSG,
                new JsonObject(jsonObject.getString("message")), new DeliveryOptions().addHeader("msgId"
                        , jsonObject.getInteger("msgId").toString())
                        .addHeader("topic", topic)
                        .addHeader("qos", jsonObject.getString("qos"))
                        .addHeader("uid", jsonObject.getString("dst") + ":" + clientid));
    }


    /**
     * @param topic 主题
     * @Description 读取消息
     * @author zhang bo
     * @date 18-7-25
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    @Override
    public void readLog(String topic, String custor) {
        logger.debug("params start custor -> {} , topic -> {} ", custor, topic);
        long startTime = System.currentTimeMillis();
        //取出信息条数,防止一次取出数据过大,阻塞
        vertx.executeBlocking(e -> {
            String index;
            switch (custor) {
                case "":
                    index = "0";
                    break;
                case "0":
                    return;
                default:
                    index = custor;
                    break;
            }
            redisClient.hscan(RedisKey.LOG_RECODR + topic, index, new ScanOptions().setCount(page_num), rs -> {
                if (rs.failed()) {
                    logger.error(rs.cause().getMessage(), rs);
                } else {
                    if (rs.result().size() > 1 && rs.result().getJsonArray(1).size() > 0) {//迭代器未結束
                        TopicPartition topicPartition = new TopicPartition().setTopic(topic);
                        consumer.assign(topicPartition);
                        String newCustor = rs.result().getString(0);
                        List<Long> offsets = getOffsets(rs.result(), topic);
                        if (offsets.size() > 0) {
                            Future.<Void>future(f -> consumer.seek(topicPartition, offsets.get(0)
                                    , f)).setHandler(res -> {
                                if (res.failed()) {
                                    logger.error(res.cause().getMessage(), res);
                                } else {
                                    consumer.handler(record -> {
                                        logger.debug("msgUser -> {} , offsets -> {}", topic, String.join(",", offsets.stream().map(val -> val.toString()).collect(Collectors.toList())));
                                        logger.debug("Processing msgUser -> {} , topic -> {} , key -> {} , value -> {} , partition -> {} , offset -> {} ", topic
                                                , record.topic(), record.key(), record.value(), record.partition(), record.offset());
                                        if (offsets.contains(record.offset())) {
                                            consumer.commit();
                                            //推送消息
                                            publishMsg(record.value(), topic);
                                        }
                                    });
                                }
                            });
                            if (!newCustor.equals("0")) {
                                readLog(topic, newCustor);
                            } else {
                                logger.debug("params end custor -> {} , topic -> {} , time -> {}", custor, topic, (System.currentTimeMillis() - startTime));
                                e.complete();
                            }
                        }
                    }
                }
            });
        }, false, null);
    }


    /**
     * @Description 消息MSG Id
     * @author zhang bo
     * @date 18-7-27
     * @version 1.0
     */
    public void readLog(Message<JsonObject> message) {
        VerifyParamsUtil.verifyParams(message.body(), new JsonObject().put("topic", DataType.STRING)
                , rs -> {
                    if (rs.failed()) {
                        message.fail(401, "params verify fail");
                    } else {
                        readLog(rs.result().getString("topic"), "");
                    }
                });
    }


    /**
     * @Description 获取offset
     * @author zhang bo
     * @date 18-7-30
     * @version 1.0
     */
    public void msgExists(Message<JsonObject> message) {
        VerifyParamsUtil.verifyParams(message.body(), new JsonObject().put("msgId", DataType.INTEGER).put("topic", DataType.STRING), rs -> {
            if (rs.failed()) {
                message.fail(401, "params verify fail");
            } else {
                redisClient.hget(RedisKey.LOG_RECODR + rs.result().getString("topic"), rs.result().getString("msgId"), msg -> {
                    if (msg.failed()) {
                        logger.error(msg.cause().getMessage(), msg);
                    } else {
                        if (Objects.nonNull(msg.result()))
                            message.reply(false);
                        else
                            message.reply(true);
                    }
                });
            }
        });
    }

    /**
     * @param msgId 消息MSG Id
     * @param topic 主题
     * @Description 消費消息
     * @author zhang bo
     * @date 18-7-25
     * @version 1.0
     */
    @Override
    public void consumeLog(int msgId, String topic, Message<JsonObject> message) {
        logger.debug("params  msg -> {} , topic -> {}", msgId, topic);
        redisClient.hget(RedisKey.LOG_RECODR + topic, String.valueOf(msgId), res -> {
            if (res.failed()) {
                res.cause().printStackTrace();
                message.reply(null);
            } else {
                if (Objects.nonNull(res.result())) {
                    redisClient.hdel(RedisKey.LOG_RECODR + topic, String.valueOf(msgId), rs -> {
                        if (rs.failed()) logger.error(rs.cause().getMessage(), rs);
                    });
                    String[] arr = res.result().split(":");
                    message.reply(arr[1]);
                } else {
                    message.reply(null);
                }
            }
        });

    }


    public void consumeLog(Message<JsonObject> message) {
        VerifyParamsUtil.verifyParams(message.body(), new JsonObject().put("msgId", DataType.INTEGER).put("topic", DataType.STRING)
                , rs -> {
                    if (rs.failed()) {
                        message.fail(401, "params verify fail");
                    } else {
                        consumeLog(rs.result().getInteger("msgId"), rs.result().getString("topic"), message);
                    }
                });
    }


    /**
     * @Description 存储释放消息Id
     * @author zhang bo
     * @date 18-8-1
     * @version 1.0
     */
    public void saveRelMsgId(Message<JsonObject> message) {
        VerifyParamsUtil.verifyParams(message.body(), new JsonObject().put("pubRelId", DataType.INTEGER).put("topic", DataType.STRING)
                .put("timeId", DataType.LONG), rs -> {
            if (rs.failed()) {
                message.fail(401, "params verify fail");
                message.reply(false);
            } else {
                redisClient.hset(RedisKey.LOG_PUBREL + rs.result().getString("topic"), rs.result().getInteger("pubRelId").toString()
                        , String.valueOf(rs.result().getLong("timeId")), as -> {
                            if (as.failed()) {
                                logger.error(as.cause().getMessage(), as);
                                message.reply(false);
                            } else {
                                if (as.result() > 0)
                                    message.reply(true);
                                else
                                    message.reply(false);
                            }
                        });
                //设置存活时间
                redisClient.expire(RedisKey.LOG_PUBREL + rs.result().getString("topic"), expire, as -> {
                    if (as.failed()) logger.error(as.cause().getMessage(), as);
                });
            }
        });
    }


    /**
     * @Description 湖區所有的pubrel
     * @author zhang bo
     * @date 18-8-1
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public List<String> getPubRels(JsonArray jsonArray, String topic) {
        JsonArray resJsonArray = jsonArray.getJsonArray(1);
        if (jsonArray.size() > 0) {
            JsonArray jsons = new JsonArray();
            for (int i = 0; i < resJsonArray.size(); i = i + 2) {
                jsons.add(resJsonArray.getString(i));
            }
            return jsons.getList();
        } else {
            return new ArrayList<>(0);
        }
    }

    /**
     * @Description 消费Id
     * @author zhang bo
     * @date 18-8-1
     * @version 1.0
     */
    public void consumerRel(Message<JsonObject> message) {
        VerifyParamsUtil.verifyParams(message.body(), new JsonObject().put("topic", DataType.STRING)
                        .put("relId", DataType.INTEGER)
                , rs -> {
                    if (rs.failed()) {
                        message.fail(401, "params verify fail");
                    } else {
                        redisClient.hget(RedisKey.LOG_PUBREL + rs.result().getString("topic"), rs.result().getInteger("relId").toString(), as -> {
                            if (as.failed()) {
                                message.reply(null);
                            } else {
                                message.reply(as.result());
                                redisClient.hdel(RedisKey.LOG_PUBREL + rs.result().getString("topic"), rs.result().getInteger("relId").toString(), res -> {
                                    if (res.failed()) logger.error(res.cause().getMessage(), res);
                                });
                            }
                        });
                    }
                });
    }

    /**
     * @Description 推送釋放rel ID
     * @author zhang bo
     * @date 18-8-1
     * @version 1.0
     */
    public void publishRelByMsg(String val, String clientid) {
        vertx.eventBus().send(MessageAddr.class.getName() + SEND_PUBREL_MSG,
                new JsonObject().put("relId", val).put("clientid", clientid));
    }


    /**
     * @Description 存储释放消息Id
     * @author zhang bo
     * @date 18-8-1
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void readRelMsgId(String topic, String custor) {
        //取出信息条数,防止一次取出数据过大,阻塞
        vertx.executeBlocking(e -> {
            String index;
            switch (custor) {
                case "":
                    index = "0";
                    break;
                case "0":
                    return;
                default:
                    index = custor;
                    break;
            }
            redisClient.hscan(RedisKey.LOG_PUBREL + topic, index, new ScanOptions().setCount(page_num), rs -> {
                if (rs.failed()) {
                    logger.error(rs.cause().getMessage(), rs);
                } else {
                    if (rs.result().size() > 1 && rs.result().getJsonArray(1).size() > 0) {//迭代器未結束
                        TopicPartition topicPartition = new TopicPartition().setTopic(topic);
                        consumer.assign(topicPartition);
                        String newCustor = rs.result().getString(0);
                        List<String> pubRels = getPubRels(rs.result(), topic);
                        pubRels.stream().forEach(relId -> publishRelByMsg(relId, topic));
                        if (!newCustor.equals("0")) {
                            readRelMsgId(topic, newCustor);
                        } else {
                            e.complete();
                        }
                    }
                }
            });
        }, false, null);
    }

    /**
     * @Description 存储释放消息Id
     * @author zhang bo
     * @date 18-8-1
     * @version 1.0
     */
    public void readRelMsgId(Message<JsonObject> message) {
        VerifyParamsUtil.verifyParams(message.body(), new JsonObject().put("topic", DataType.STRING)
                , rs -> {
                    if (rs.failed()) {
                        message.fail(401, "params verify fail");
                    } else {
                        readRelMsgId(rs.result().getString("topic"), "");
                    }
                });
    }


    /**
     * @param topic 主题s
     * @Description
     * @author zhang bo
     * @date 18-7-25
     * @version 1.0
     */
    @Override
    public void deleteLog(String topic) {

    }
}
