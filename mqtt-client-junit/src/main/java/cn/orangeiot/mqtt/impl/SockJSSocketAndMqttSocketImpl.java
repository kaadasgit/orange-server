package cn.orangeiot.mqtt.impl;

import cn.orangeiot.common.genera.ErrorType;
import cn.orangeiot.util.DataType;
import cn.orangeiot.util.Result;
import cn.orangeiot.util.VerifyParamsUtil;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.web.handler.sockjs.SockJSSocket;
import io.vertx.mqtt.MqttClient;
import io.vertx.mqtt.MqttClientOptions;
import io.vertx.mqtt.messages.MqttPublishMessage;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-07-01
 */
public class SockJSSocketAndMqttSocketImpl {

    private SockJSSocket sockJSSocket;

    private static Logger logger = LogManager.getLogger(SockJSSocketAndMqttSocketImpl.class);

    private int state = 0;//连接状态 0 未连接  1 已连接不验证

    private Vertx vertx;

    private MqttClient client;

    public SockJSSocketAndMqttSocketImpl(SockJSSocket sockJSSocket, Vertx vertx) {
        this.sockJSSocket = sockJSSocket;
        this.vertx = vertx;
    }

    public void start() {
        sockJSSocket.handler(this::process);
        sockJSSocket.endHandler(this::end);
        sockJSSocket.exceptionHandler(this::exception);
    }

    /**
     * @Description 处理
     * @author zhang bo
     * @date 18-7-1
     * @version 1.0
     */
    private void process(Buffer buffer) {
        JsonObject jsonObject;
        try {
            jsonObject = new JsonObject(new String(buffer.getBytes()));
            logger.info("received data -> " + jsonObject.toString());
        } catch (Exception e) {
            paramsFail(false);
            return;
        }

        if (state == 0) {//是否連接狀態
            VerifyParamsUtil.verifyParams(jsonObject, new JsonObject().put("username", DataType.STRING)
                    .put("password", DataType.STRING).put("clientId", DataType.STRING)
                    .put("topics", DataType.JSONARRAY).put("host", DataType.STRING), rs -> {
                if (rs.failed()) {
                    logger.error(rs.cause().getMessage(), rs);
                    paramsFail(false);
                } else {
                    state = 1;//連接狀態
                    this.createMqttClient(rs.result());
                }
            });
        } else {
            VerifyParamsUtil.verifyParams(jsonObject, new JsonObject().put("qos", DataType.INTEGER)
                    .put("payload", DataType.STRING).put("topic", DataType.STRING), rs -> {
                if (rs.failed()) {
                    logger.error(rs.cause().getMessage(), rs);
                    paramsFail(false);
                } else {
                    mqttPublish(rs.result());
                }
            });
        }
    }


    /**
     * @Description mqtt notify
     * @author zhang bo
     * @date 18-7-1
     * @version 1.0
     */
    private void mqttPublish(JsonObject msg) {
        String params = msg.getString("payload").replace("\\s", "").replace("\n", "");
        // 消息发布到订阅主题
        client.publish(
                msg.getString("topic"),
                Buffer.buffer(params),
                MqttQoS.valueOf(msg.getInteger("qos")),
                false,
                false,
                s -> logger.info("Publish sent to a server , topic -> {} , content ->{] , qos -> {} , packageMsgId -> {}"
                        , msg.getString("topic"), msg.getString("content"), msg.getInteger("qos"), s));
    }


    /**
     * @Description 推送web客户端
     * @author zhang bo
     * @date 18-7-1
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    private void publishWebClient(JsonObject msg) {
        try {
            sockJSSocket.write(msg.toString());
            if (sockJSSocket.writeQueueFull()) {
                sockJSSocket.pause();
                sockJSSocket.drainHandler(done -> sockJSSocket.resume());
            }
        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
        }
    }


    /**
     * @Description 參數錯誤
     * @author zhang bo
     * @date 18-7-1
     * @version 1.0
     */
    private void paramsFail(boolean flag) {
        publishCloneOrException(JsonObject.mapFrom(new Result<JsonObject>().setErrorMessage(ErrorType.RESULT_PARAMS_FAIL.getKey()
                , ErrorType.RESULT_DATA_FAIL.getValue())), flag);
    }


    /**
     * @Description 推送web客户端
     * @author zhang bo
     * @date 18-7-1
     * @version 1.0
     */
    private void publishCloneOrException(JsonObject msg, boolean flag) {
        try {
            sockJSSocket.write(msg.toString());
            if (sockJSSocket.writeQueueFull()) {
                sockJSSocket.pause();
                sockJSSocket.drainHandler(done -> sockJSSocket.resume());
            }
        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
        } finally {
            if (flag)
                sockJSSocket.close();
        }
    }


    /**
     * @Description 关闭连接
     * @author zhang bo
     * @date 18-7-1
     * @version 1.0
     */
    private void end(Void socket) {
        sockJSSocket = null;
        if (client != null)
            client.disconnect();
    }


    /**
     * @Description 异常
     * @author zhang bo
     * @date 18-7-1
     * @version 1.0
     */
    private void exception(Throwable throwable) {
        sockJSSocket = null;
        if (client != null)
            client.disconnect();
        logger.error(throwable.getMessage(), throwable);
    }

    /**
     * @param certPath 证书路径
     * @Description 证书写入缓冲区
     * @author zhang bo
     * @date 19-1-11
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public Buffer getCert(String certPath) {
        InputStream jksIn = SockJSSocketAndMqttSocketImpl.class.getResourceAsStream(certPath);
        Buffer buffer = null;
        try {
            if (jksIn != null) {
                byte[] jksByte = IOUtils.toByteArray(jksIn);
                buffer = Buffer.buffer().appendBytes(jksByte);
            }
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
        return buffer;
    }


    /**
     * @Description 加载TLS
     * @author zhang bo
     * @date 19-1-11
     * @version 1.0
     */
    public void loadTls(MqttClientOptions options) {
        String bol = System.getProperty("tls");
        Boolean flag = false;
        try {
            if (bol != null) {
                flag = Boolean.valueOf(bol);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Buffer bu = null;
        if (flag && (bu = getCert("/server.jks")) != null) {
            options.setSsl(true).setKeyStoreOptions(new JksOptions().setValue(bu)
                    .setPassword("123456"));
        }
    }

    /**
     * @Description 创建mqtt客户端
     * @author zhang bo
     * @date 18-7-1
     * @version 1.0
     */
    public void createMqttClient(JsonObject jsonObject) {
        Map<String, Integer> topics = new HashMap<>();
        try {
            jsonObject.getJsonArray("topics").forEach(e -> {
                JsonObject map = new JsonObject(e.toString());
                topics.put(map.getString("topic"), map.getInteger("qos"));
            });
        } catch (Exception e) {
            publishWebClient(JsonObject.mapFrom(new Result<JsonObject>().setErrorMessage(ErrorType.RESULT_PARAMS_FAIL.getKey()
                    , ErrorType.RESULT_DATA_FAIL.getValue())));
            return;
        }
        if (jsonObject.getString("host").indexOf(":") <= 0) {
            publishWebClient(JsonObject.mapFrom(new Result<JsonObject>().setErrorMessage(ErrorType.RESULT_PARAMS_FAIL.getKey()
                    , ErrorType.RESULT_DATA_FAIL.getValue())));
            return;
        }

        MqttClientOptions options = new MqttClientOptions().setPassword(jsonObject.getString("password"))
                .setUsername(jsonObject.getString("username")).setKeepAliveTimeSeconds(10).setClientId(jsonObject.getString("clientId"))
                .setCleanSession(true);

        loadTls( options);

        String[] host = jsonObject.getString("host").split(":");
        client = MqttClient.create(vertx, options);
        // 连接服务器
        client.connect(Integer.parseInt(host[1]), host[0], connect -> {
            if (connect.failed()) {
                publishWebClient(JsonObject.mapFrom(new Result<JsonObject>().setErrorMessage(ErrorType.MQTT_CONNECT_FAIL.getKey()
                        , ErrorType.MQTT_CONNECT_FAIL.getValue())));
                logger.error(connect.cause().getMessage(), connect.cause());
                publishWebClient(JsonObject.mapFrom(new Result<JsonObject>().setErrorMessage(500, "CONNECTION_REFUSED_NOT_AUTHORIZED"))
                );//连接成功
                sockJSSocket.close();
            } else
                client.subscribe(topics);
        }).subscribeCompletionHandler(ackMsg -> {
            publishWebClient(JsonObject.mapFrom(new Result<JsonObject>()));//连接成功
        }).publishHandler(this::mqttPublish);
    }

    /**
     * @Description mqtt publish 处理
     * @author zhang bo
     * @date 18-7-1
     * @version 1.0
     */
    public void mqttPublish(MqttPublishMessage publishMessage) {
        logger.info("Just received message on [" + publishMessage.topicName() + "] payload ["
                + publishMessage.payload().toString(Charset.defaultCharset()) + "] with QoS [" + publishMessage.qosLevel() + "]");
        if (Objects.nonNull(publishMessage)) {
            JsonObject jsonObject = new JsonObject().put("topic", publishMessage.topicName()).put("payload"
                    , new JsonObject(publishMessage.payload().toString(Charset.defaultCharset()))).put("qos", publishMessage.qosLevel().value());
            this.publishWebClient(JsonObject.mapFrom(new Result<JsonObject>().setData(jsonObject)));
        }
    }

}
