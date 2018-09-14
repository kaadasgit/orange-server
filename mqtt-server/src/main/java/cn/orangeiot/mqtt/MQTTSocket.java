package cn.orangeiot.mqtt;

import cn.orangeiot.common.genera.Result;
import cn.orangeiot.common.options.SendOptions;
import cn.orangeiot.mqtt.parser.MQTTEncoder;
import cn.orangeiot.mqtt.prometheus.PromMetrics;
import cn.orangeiot.mqtt.parser.MQTTDecoder;
import cn.orangeiot.mqtt.util.QOSConvertUtils;
import cn.orangeiot.reg.EventbusAddr;
import cn.orangeiot.reg.gateway.GatewayAddr;
import cn.orangeiot.reg.log.LogAddr;
import cn.orangeiot.reg.message.MessageAddr;
import cn.orangeiot.reg.storage.StorageAddr;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetSocket;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.dna.mqtt.moquette.proto.messages.*;
import scala.util.parsing.json.JSONObject;
import sun.rmi.runtime.Log;

import javax.mail.Session;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.dna.mqtt.moquette.proto.messages.AbstractMessage.*;
import static org.dna.mqtt.moquette.proto.messages.AbstractMessage.QOSType.EXACTLY_ONCE;
import static org.dna.mqtt.moquette.proto.messages.AbstractMessage.QOSType.LEAST_ONE;
import static org.dna.mqtt.moquette.proto.messages.AbstractMessage.QOSType.MOST_ONE;

/**
 * Created by giovanni on 07/05/2014.
 * Base class for connection handling, 1 tcp connection corresponds to 1 instance of this class.
 */
public abstract class MQTTSocket implements MQTTPacketTokenizer.MqttTokenizerListener, Handler<Buffer>, EventbusAddr {

    private static Logger logger = LogManager.getLogger(MQTTSocket.class);

    private final int DEFAULT_SENDMSG_TIMES = 5;//发送次数

    protected Vertx vertx;
    private MQTTDecoder decoder;
    private MQTTEncoder encoder;
    private MQTTPacketTokenizer tokenizer;
    protected MQTTSession session;
    private ConfigParser config;
    private Map<String, MQTTSession> sessions;
    private NetSocket netSocket;
    private int sendTimes;
    private final String USER_PREFIX = "app:";//用戶前綴
    private final String GATEWAY_PREFIX = "gw:";//网关前綴
    private final String GATEWAY_ON_OFF_STATE = "gatewayState";//網關狀態
    private final String REPLY_MESSAGE = "/clientId/rpc/reply";


    public MQTTSocket(Vertx vertx, ConfigParser config, Map<String, MQTTSession> sessions, NetSocket netSocket) {
        this.decoder = new MQTTDecoder();
        this.encoder = new MQTTEncoder();
        this.tokenizer = new MQTTPacketTokenizer();
        this.tokenizer.registerListener(this);
        this.vertx = vertx;
        this.config = config;
        this.sessions = sessions;
        this.netSocket = netSocket;
        this.sendTimes = DEFAULT_SENDMSG_TIMES;
    }

    public MQTTSocket(Vertx vertx, ConfigParser config, Map<String, MQTTSession> sessions) {
        this.decoder = new MQTTDecoder();
        this.encoder = new MQTTEncoder();
        this.tokenizer = new MQTTPacketTokenizer();
        this.tokenizer.registerListener(this);
        this.vertx = vertx;
        this.config = config;
        this.sessions = sessions;
        this.sendTimes = DEFAULT_SENDMSG_TIMES;
    }

    abstract protected void sendMessageToClient(Buffer bytes);

    abstract protected void closeConnection();

    public void shutdown() {
        if (tokenizer != null) {
            tokenizer.removeAllListeners();
            tokenizer = null;
        }
        if (session != null) {
            removeCurrentSessionFromMap();
            session.shutdown();
            session = null;
        }
        vertx = null;
    }

    @Override
    public void handle(Buffer buffer) {
        tokenizer.process(buffer.getBytes());
    }


    @Override
    public void onToken(byte[] token, boolean timeout) throws Exception {
        try {
            if (!timeout) {
                Buffer buffer = Buffer.buffer(token);
                AbstractMessage message = decoder.dec(buffer);
                onMessageFromClient(message);
                sendMessage();
                sendGWMessage();
                sendStorage();
                getloginAll();
                sendPubRel();
                kickOut();
                deviceState();
            } else {
                logger.warn("Timeout occurred ...");
            }
        } catch (Throwable ex) {
            String clientInfo = getClientInfo();
            logger.error(clientInfo + ", Bad error in processing the message", ex);
            closeConnection();
        }
    }

    @Override
    public void onError(Throwable e) {
        String clientInfo = getClientInfo();
        logger.error(clientInfo + ", " + e.getMessage(), e);
//        if(e instanceof CorruptedFrameException) {
        closeConnection();
//        }
    }

    @SuppressWarnings("Duplicates")
    private void onMessageFromClient(AbstractMessage msg) throws Exception {
        logger.debug("<<< " + msg);
        switch (msg.getMessageType()) {
            case CONNECT:
                ConnectMessage connect = (ConnectMessage) msg;
                ConnAckMessage connAck = new ConnAckMessage();
                String connectedClientID = connect.getClientID();
                PromMetrics.mqtt_connect_total.labels(connectedClientID).inc();
//                if (!connect.isCleanSession() && sessions.containsKey(connectedClientID)) {
                if (sessions.containsKey(connectedClientID)) {
                    session = sessions.get(connectedClientID);
                }
                //重复连接
                if (session == null) {
                    session = new MQTTSession(vertx, config, netSocket);
                    session.setClientID(connectedClientID);
                    PromMetrics.mqtt_sessions_total.inc();
                    connAck.setSessionPresent(false);
                } else {
                    logger.debug("Session alredy allocated ...");
                    /*
                     The Server MUST process a second CONCT Packet sent from a Client as a protocol violation and disconnect the Client
                      */
//                    connAck.setSessionPresent(true);
//                    connAck.setReturnCode(ConnAckMessage.NOT_AUTHORIZED);
//                    sendMessageToClient(connAck);
//                    closeConnection();
//                    break;

                    /**挤掉上一个用户*/
                    sessions.remove(connectedClientID);
                    session.closeConnect();

                    session = new MQTTSession(vertx, config, netSocket);
                    session.setClientID(connectedClientID);
                    PromMetrics.mqtt_sessions_total.inc();
                    connAck.setSessionPresent(false);
                }
                session.setPublishMessageHandler(this::sendMessageToClient);
                session.setKeepaliveErrorHandler(clientID -> {
                    String cinfo = clientID;
                    if (session != null) {
                        cinfo = session.getClientInfo();
                    }
                    logger.debug("keep alive exausted! closing connection for client[" + cinfo + "] ...");
                    checkDevice(connectedClientID, "offline");//离线狀態
                    closeConnection();
                });
                session.handleConnectMessage(connect, authenticated -> {
                    if (authenticated.getBoolean("state")) {
                        connAck.setReturnCode(ConnAckMessage.CONNECTION_ACCEPTED);
                        sessions.put(session.getClientID(), session);
                        sendMessageToClient(connAck);
                        if (!session.isCleanSession()) {
                            session.sendAllMessagesFromQueue();
                        }


                    } else {
                        logger.warn("Authentication failed! clientID= " + connect.getClientID() + " username=" + connect.getUsername());
//                        closeConnection();
                        if (Objects.nonNull(authenticated.getString("header"))) {
                            connAck.setReturnCode(ConnAckMessage.NOT_AUTHORIZED);
                            sendMessageToClient(connAck);
                        } else {
                            connAck.setReturnCode(ConnAckMessage.BAD_USERNAME_OR_PASSWORD);
                            sendMessageToClient(connAck);
                        }
                    }
                });
                break;
            case SUBSCRIBE:
                session.resetKeepAliveTimer();

                SubscribeMessage subscribeMessage = (SubscribeMessage) msg;
                PromMetrics.mqtt_subscribe_total.labels(session.getClientID()).inc();

                session.handleSubscribeMessage(subscribeMessage, permitted -> {
                    SubAckMessage subAck = new SubAckMessage();
                    subAck.setMessageID(subscribeMessage.getMessageID());
                    int indx = 0;
                    for (SubscribeMessage.Couple c : subscribeMessage.subscriptions()) {
                        if (permitted.getBoolean(indx++)) {
                            QOSType qos = new QOSUtils().toQos(c.getQos());
                            subAck.addType(qos);
                        } else {
                            subAck.addType(QOSType.FAILURE);
                        }
                    }
                    if (subscribeMessage.isRetainFlag()) {
                        /*
                        When a new subscription is established on a topic,
	                    the last retained message on that topic should be sent to the subscriber with the Retain flag set.
	                    If there is no retained message, nothing is sent
	                    */
                    }
                    sendMessageToClient(subAck);

                    checkDevice(session.getClientID(), "online");//在線狀態
                    checkUser(session.getClientID(), "online");//在線狀態
                    //推送持久化记录
//                    vertx.eventBus().send(StorageAddr.class.getName() + GET_STORAGE_DATA, null
//                            , new DeliveryOptions().addHeader("clientId", session.getClientID()));

                    vertx.eventBus().send(LogAddr.class.getName() + READ_LOG, new JsonObject().put("topic", session.getClientID().split(":")[1]));
                    vertx.eventBus().send(LogAddr.class.getName() + SEND_PUBREL, new JsonObject().put("topic", session.getClientID()));
                });
                break;
            case UNSUBSCRIBE:
                PromMetrics.mqtt_unsubscribe_total.labels(session.getClientID()).inc();
                session.resetKeepAliveTimer();

                UnsubscribeMessage unsubscribeMessage = (UnsubscribeMessage) msg;
                session.handleUnsubscribeMessage(unsubscribeMessage);
                UnsubAckMessage unsubAck = new UnsubAckMessage();
                unsubAck.setMessageID(unsubscribeMessage.getMessageID());
                sendMessageToClient(unsubAck);
                break;
            case PUBLISH:
                session.resetKeepAliveTimer();

                PublishMessage publish = (PublishMessage) msg;

                logger.debug("client publish topic -> {}", publish.getTopicName());

                session.handlerPublishMessage(publish, session.getClientID(), rs -> {
                    if (rs.failed()) {
                        logger.error(rs.cause().getMessage());
                        publishReplyPackage(publish);
                    } else {
                        if (Objects.nonNull(rs.result()) && Objects.nonNull(rs.result().getValue("flag")))
                            publishMsg(publish, rs.result(), false);
                        else if (Objects.nonNull(rs.result()))
                            publishMsg(publish, rs.result(), true);
                    }
                });
                break;
            case PUBREC:
                session.resetKeepAliveTimer();

                PubRecMessage pubRec = (PubRecMessage) msg;
                PubRelMessage prelResp = new PubRelMessage();
                prelResp.setMessageID(pubRec.getMessageID());
                prelResp.setQos(LEAST_ONE);
//                sendMessageToClient(prelResp);

                //停止发送
                vertx.eventBus().send(LogAddr.class.getName() + CONSUME_LOG, new JsonObject().put("msgId", pubRec.getMessageID())
                        .put("topic", session.getClientID().split(":")[1]), (AsyncResult<Message<String>> rs) -> {
                    if (rs.failed()) {
                        logger.error(rs.cause().getMessage(), rs);
                    } else {
                        if (Objects.nonNull(rs.result().body())) {
                            vertx.cancelTimer(Long.parseLong(rs.result().body()));
                            saveRel(prelResp);
                        }
                    }
                });


                break;
            case PUBREL:
                session.resetKeepAliveTimer();
                PubRelMessage pubRel = (PubRelMessage) msg;
                PubCompMessage pubComp = new PubCompMessage();
                pubComp.setMessageID(pubRel.getMessageID());
                sendMessageToClient(pubComp);

                //接收确认
//                vertx.eventBus().send(StorageAddr.class.getName() + DEL_STORAGE_DATA, null, new DeliveryOptions()
//                        .addHeader("clientId", session.getClientID()).addHeader("msgId", pubRel.getMessageID().toString()));
                break;
            case PUBACK:
                session.getMessageFromQueue();
                session.resetKeepAliveTimer();
                PubAckMessage pubAckMessage = (PubAckMessage) msg;

                //接收确认
//                vertx.eventBus().send(StorageAddr.class.getName() + DEL_STORAGE_DATA, null, new DeliveryOptions()
//                        .addHeader("clientId", session.getClientID()).addHeader("msgId", pubAckMessage.getMessageID().toString()));

                vertx.eventBus().send(LogAddr.class.getName() + CONSUME_LOG, new JsonObject().put("msgId", pubAckMessage.getMessageID())
                        .put("topic", session.getClientID().split(":")[1]), (AsyncResult<Message<String>> rs) -> {
                    if (rs.failed()) {
                        logger.error(rs.cause().getMessage(), rs);
                    } else {
                        if (Objects.nonNull(rs.result().body())) vertx.cancelTimer(Long.parseLong(rs.result().body()));
                    }
                });
                // A PUBACK message is the response to a PUBLISH message with QoS level 1.
                // A PUBACK message is sent by a server in response to a PUBLISH message from a publishing client,
                // and by a subscriber in response to a PUBLISH message from the server.
                break;
            case PUBCOMP:
                session.getMessageFromQueue();
                session.resetKeepAliveTimer();

                PubCompMessage pubCompMessage = (PubCompMessage) msg;
                vertx.eventBus().send(LogAddr.class.getName() + CONSUME_PUBREL, new JsonObject().put("relId", pubCompMessage.getMessageID())
                        .put("topic", session.getClientID()), (AsyncResult<Message<String>> rs) -> {
                    if (rs.failed()) {
                        logger.error(rs.cause().getMessage(), rs);
                    } else {
                        if (Objects.nonNull(rs.result().body())) vertx.cancelTimer(Long.parseLong(rs.result().body()));
                    }
                });
                break;
            case PINGREQ:
                session.resetKeepAliveTimer();
                PingRespMessage pingResp = new PingRespMessage();
                sendMessageToClient(pingResp);
                break;
            case DISCONNECT:
                PromMetrics.mqtt_disconnect_total.labels(session.getClientID()).inc();
                checkDevice(session.getClientID(), "offline");//离线狀態
                session.resetKeepAliveTimer();
                DisconnectMessage disconnectMessage = (DisconnectMessage) msg;
                handleDisconnect(disconnectMessage);
                break;
            default:
                logger.warn("type of message not known: " + msg.getClass().getSimpleName());
                break;
        }


        // : forward mqtt message to backup server

    }


    /**
     * @Description
     * @author zhang bo
     * @date 18-9-10
     * @version 1.0
     */
    public void checkDevice(String clientId, String state) {
        if (clientId.indexOf(GATEWAY_PREFIX) >= 0) {//網關
            String gwId = clientId.split(":")[1];
            vertx.eventBus().send(GatewayAddr.class.getName() + GET_GATWWAY_USERALL, new JsonObject().put("clientId", gwId)
                    , SendOptions.getInstance(), (AsyncResult<Message<JsonArray>> ars) -> {
                        if (ars.failed()) {
                            logger.error(ars.cause().getMessage(), ars);
                        } else {
                            sendDeviceState(gwId, ars.result().body(), state, false);
                        }
                    });
        }
    }


    public void checkUser(String clientId, String state) {
        if (clientId.indexOf(USER_PREFIX) >= 0) {//網關
            String userId = clientId.split(":")[1];
            vertx.eventBus().send(GatewayAddr.class.getName() + GET_USER_GATEWAYLIST, new JsonObject().put("clientId", userId)
                    , SendOptions.getInstance(), (AsyncResult<Message<JsonArray>> ars) -> {
                        if (ars.failed()) {
                            logger.error(ars.cause().getMessage(), ars);
                        } else {
                            sendDeviceState(userId, ars.result().body(), state, true);
                        }
                    });
        }
    }

    /**
     * @Description 設備狀態
     * @author zhang bo
     * @date 18-9-13
     * @version 1.0
     */
    public void deviceState() {
        vertx.eventBus().consumer(GatewayAddr.class.getName() + SEND_GATEWAY_STATE, (Message<JsonObject> rs) -> {
            if (Objects.nonNull(rs.body()) && Objects.nonNull(rs.body().getValue("_id")) && Objects.nonNull(rs.body().getValue("gwId")))
                sendDeviceState(rs.body().getString("_id"), new JsonArray().add(new JsonObject().put("deviceSN", rs.body().getString("gwId"))
                        .put("uid", rs.body().getString("_id"))), "online", true);
        });
    }


    /**
     * @param gwId  客户端连接id
     * @param state 状态
     * @param flag  狀態
     * @Description 發送設備狀態
     * @author zhang bo
     * @date 18-9-10
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void sendDeviceState(String gwId, JsonArray jsonArray, String state, boolean flag) {
        if (Objects.nonNull(jsonArray) && jsonArray.size() > 0) {
            for (int i = 0; i < jsonArray.size(); i++) {
                JsonObject jsonObject = jsonArray.getJsonObject(i);
                PublishMessage publishMessage = new PublishMessage();
                publishMessage.setQos(QOSType.MOST_ONE);
                publishMessage.setRetainFlag(false);
                publishMessage.setDupFlag(false);
                publishMessage.setTopicName(REPLY_MESSAGE.replace("clientId", jsonObject.getString("uid")));
                try {
                    JsonObject sendJsonObject = new JsonObject().put("func", GATEWAY_ON_OFF_STATE).put("data", new JsonObject().put("state", state));
                    if (flag) {
                        sendJsonObject.put("devuuid", jsonObject.getString("deviceSN"));
                        MQTTSession mqttSession = null;
                        if (Objects.nonNull(mqttSession = sessions.get(GATEWAY_PREFIX + jsonObject.getString("deviceSN")))) {
                            sendJsonObject.getJsonObject("data").put("state", "online");
                            publishMessage.setPayload(sendJsonObject.toString());
                            if (session != null)
                                session.sendMessageToClient(publishMessage);
                            else {
                                MQTTSession mqttSession1;
                                if (Objects.nonNull(mqttSession1 = sessions.get(USER_PREFIX + jsonObject.getString("uid"))))
                                    mqttSession1.sendMessageToClient(publishMessage);
                            }
                        } else {
                            sendJsonObject.getJsonObject("data").put("state", "offline");
                            publishMessage.setPayload(sendJsonObject.toString());
                            if (session != null)
                                session.sendMessageToClient(publishMessage);
                            else {
                                MQTTSession mqttSession1;
                                if (Objects.nonNull(mqttSession1 = sessions.get(USER_PREFIX + jsonObject.getString("uid"))))
                                    mqttSession1.sendMessageToClient(publishMessage);
                            }
                        }
                    } else {
                        sendJsonObject.put("devuuid", gwId);
                        publishMessage.setPayload(sendJsonObject.toString());
                        MQTTSession mqttSession = null;
                        if (Objects.nonNull(mqttSession = sessions.get(USER_PREFIX + jsonObject.getString("uid"))))
                            mqttSession.sendMessageToClient(publishMessage);
                    }
                } catch (UnsupportedEncodingException e1) {
                    logger.error(e1.getCause().getMessage(), e1);
                }
            }
        }
    }


    /**
     * @Description 保存rel包
     * @author zhang bo
     * @date 18-8-1
     * @version 1.0
     */
    public void saveRel(PubRelMessage prel) {
        //保存relid
        AtomicInteger atomicInteger = new AtomicInteger(0);
        Long timeId = vertx.setPeriodic(2000, id -> {
            sendMessageToClient(prel);
            atomicInteger.incrementAndGet();
            if (atomicInteger.get() == sendTimes) {
                vertx.cancelTimer(id);
            }
        });
        vertx.eventBus().send(LogAddr.class.getName() + SAVE_PUBREL, new JsonObject().put("pubRelId", prel.getMessageID())
                .put("topic", session.getClientID()).put("timeId", timeId), (AsyncResult<Message<Boolean>> rs) -> {
            if (rs.failed()) {
                vertx.cancelTimer(timeId);
            } else {
                if (!rs.result().body())
                    vertx.cancelTimer(timeId);
                sendMessageToClient(prel);
            }
        });
    }


    /**
     * @Description 回复确认包
     * @author zhang bo
     * @date 18-6-29
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void publishReplyPackage(PublishMessage publish) {
        switch (publish.getQos()) {
            case RESERVED:
                session.handlePublishMessage(publish, null);
                break;
            case MOST_ONE:
                session.handlePublishMessage(publish, null);
                break;
            case LEAST_ONE:
                PubAckMessage pubAck = new PubAckMessage();
                pubAck.setMessageID(publish.getMessageID());
                sendMessageToClient(pubAck);
                break;
            case EXACTLY_ONCE:
                PubRecMessage pubRec = new PubRecMessage();
                pubRec.setMessageID(publish.getMessageID());
                sendMessageToClient(pubRec);
                break;
            default:
                logger.error("qos is mo have , qos ->" + publish.getQos());
                break;
        }
    }


    public void publishMsg(PublishMessage publish, JsonObject rs, boolean flag) {
        if (Objects.nonNull(rs)) {
            String tempTopic = publish.getTopicName();
            QOSType qos = publish.getQos();
            publish.setTopicName(rs.getString("topicName"));
            if (flag)
                try {
                    rs.remove("topicName");
                    publish.setPayload(rs.toString());
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            String topic = Objects.nonNull(publish.getTopicName()) ? publish.getTopicName() : tempTopic;
            PromMetrics.mqtt_publish_total.labels(session.getClientID(), qos.name(), topic).inc();
            switch (publish.getQos()) {
                case RESERVED:
                    session.handlePublishMessage(publish, null);
                    break;
                case MOST_ONE:
                    session.handlePublishMessage(publish, null);
                    break;
                case LEAST_ONE:
                    session.handlePublishMessage(publish, permitted -> {
                        PubAckMessage pubAck = new PubAckMessage();
                        pubAck.setMessageID(publish.getMessageID());
                        sendMessageToClient(pubAck);
                    });
                    break;
                case EXACTLY_ONCE:
                    session.handlePublishMessage(publish, permitted -> {
                        PubRecMessage pubRec = new PubRecMessage();
                        pubRec.setMessageID(publish.getMessageID());
                        sendMessageToClient(pubRec);
                    });
                    break;
            }
        }
    }


    //踢出
    public void kickOut() {
        vertx.eventBus().consumer(MessageAddr.class.getName() + KICK_OUT, (Message<JsonObject> rs) -> {
            MQTTSession session = null;
            if (Objects.nonNull(rs.headers().get("clientId")) && Objects.nonNull(session = sessions.get(rs.headers().get("clientId")))) {
                session.closeConnect();
            }
        });
    }


    //qos发送消息
    public void sendMessage() {
        vertx.eventBus().consumer(MessageAddr.class.getName() + SEND_ADMIN_MSG, (Message<JsonObject> rs) -> {
            String topicName = SEND_USER_REPLAY.replace("clientId", rs.headers().get("uid").replace("app:", ""));
            rs.headers().set("topicName", topicName);
            sendMsgToClient(rs);
        });
    }


    //qos发送升级消息
    public void sendGWMessage() {
        vertx.eventBus().consumer(MessageAddr.class.getName() + SEND_UPGRADE_MSG, (Message<JsonObject> rs) -> {
            String topicName = rs.headers().get("topic");
            rs.headers().set("topicName", topicName);
            sendMsgToClient(rs);
        });
    }


    //发送storage消息
    public void sendStorage() {
        vertx.eventBus().consumer(MessageAddr.class.getName() + SEND_STORAGE_MSG, (Message<JsonObject> rs) -> {
            String topicName = rs.headers().get("topic");
            rs.headers().set("topicName", topicName);
            sendMsgToClient(rs);
        });
    }

    //发送pubRel消息
    public void sendPubRel() {
        vertx.eventBus().consumer(MessageAddr.class.getName() + SEND_PUBREL_MSG, (Message<JsonObject> rs) -> {
            PubRelMessage prelResp = new PubRelMessage();
            prelResp.setMessageID(Integer.parseInt(rs.body().getString("relId")));
            prelResp.setQos(LEAST_ONE);
            sendMessageToClient(prelResp);
            if (Objects.nonNull(sessions.get(rs.body().getString("clientid"))))
                sessions.get(rs.body().getString("clientid")).sendMessageToClient(prelResp);
        });
    }


    /**
     * @Description 發送消息到客戶端
     * @author zhang bo
     * @date 18-4-16
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void sendMsgToClient(Message<JsonObject> rs) {
        PublishMessage publish = new PublishMessage();
        publish.setTopicName(rs.headers().get("topicName"));
        try {
            if (Objects.nonNull(rs.headers().get("msgId")))
                publish.setMessageID(Integer.parseInt(rs.headers().get("msgId")));
            publish.setPayload(rs.body().toString());
        } catch (UnsupportedEncodingException e) {
            logger.error(e.getMessage(), e);
        }
        switch (Integer.parseInt(rs.headers().get("qos"))) {
            case 0:
                if (Objects.nonNull(session) && !Objects.nonNull(rs.headers().get("redict"))) {
                    session.handlePublishMessage(publish, null);
                } else {
                    publish.setQos(MOST_ONE);
                    publish.setMessageID(0);
                    if (rs.headers().get("uid").indexOf(":") >= 0) {
                        sessions.get(rs.headers().get("uid")).handlePublishMessage(publish, null);
                    } else {
                        sessions.get(rs.headers().get("uid").length() == 13 ? "gw:" + rs.headers().get("uid")
                                : "app:" + rs.headers().get("uid")).handlePublishMessage(publish, null);
                    }
                }
                break;
            case 1:
                publish.setQos(LEAST_ONE);
                if (Objects.nonNull(session) && !Objects.nonNull(rs.headers().get("redict"))) {
                    if (!Objects.nonNull(publish.getMessageID()))
                        publish.setMessageID(1);
                    session.handlePublishMessage(publish, permitted -> {
                        PubAckMessage pubAck = new PubAckMessage();
                        pubAck.setMessageID(publish.getMessageID());
                        if (Objects.nonNull(pubAck))
                            sendMessageToClient(pubAck);
                    });
                } else {
                    if (Objects.nonNull(rs.headers().get("messageId"))) {
                        publish.setMessageID(Integer.parseInt(rs.headers().get("messageId")));
                    } else if (Objects.nonNull(rs.headers().get("msgId"))) {
                        publish.setMessageID(Integer.parseInt(rs.headers().get("msgId")));
                    } else {
                        publish.setMessageID(1);
                    }
                    if (rs.headers().get("uid").indexOf(":") >= 0) {
                        if (Objects.nonNull(rs.headers().get("uid"))) {
                            MQTTSession mqttSession;
                            if (Objects.nonNull(mqttSession = sessions.get(rs.headers().get("uid")))) {
                                mqttSession.handlePublishMessage(publish, permitted -> {
                                    PubAckMessage pubAck = new PubAckMessage();
                                    pubAck.setMessageID(publish.getMessageID());
                                    sendMessageToClient(pubAck);
                                });
                            }else{
                                try {
                                    writeLog(rs.headers().get("uid"), publish);
                                } catch (Exception e) {
                                    logger.error(e.getMessage(), e);
                                }
                            }
                        } else
                            try {
                                writeLog(rs.headers().get("uid"), publish);
                            } catch (Exception e) {
                                logger.error(e.getMessage(), e);
                            }
                    } else {
                        String clientId = rs.headers().get("uid").length() == 13 ? "gw:" + rs.headers().get("uid")
                                : "app:" + rs.headers().get("uid");
                        if (Objects.nonNull(sessions.get(clientId))) {
                            sessions.get(clientId).handlePublishMessage(publish, permitted -> {
                                PubAckMessage pubAck = new PubAckMessage();
                                pubAck.setMessageID(publish.getMessageID());
                                sendMessageToClient(pubAck);
                            });
                        } else
                            try {
                                writeLog(rs.headers().get("uid"), publish);
                            } catch (Exception e) {
                                logger.error(e.getMessage(), e);
                            }
                    }
                }
                break;
            case 2:
                publish.setQos(EXACTLY_ONCE);
                if (Objects.nonNull(session) && !Objects.nonNull(rs.headers().get("redict"))) {
                    if (Objects.nonNull(rs.headers().get("messageId"))) {
                        publish.setMessageID(Integer.parseInt(rs.headers().get("messageId")));
                    } else {
                        publish.setMessageID(1);
                    }
                    session.handlePublishMessage(publish, permitted -> {
                        PubRecMessage pubRec = new PubRecMessage();
                        pubRec.setMessageID(publish.getMessageID());
                        if (Objects.nonNull(pubRec))
                            sendMessageToClient(pubRec);
                    });

                } else {
                    if (Objects.nonNull(rs.headers().get("messageId"))) {
                        publish.setMessageID(Integer.parseInt(rs.headers().get("messageId")));
                    } else if (Objects.nonNull(rs.headers().get("msgId"))) {
                        publish.setMessageID(Integer.parseInt(rs.headers().get("msgId")));
                    } else {
                        publish.setMessageID(1);
                    }
                    if (rs.headers().get("uid").indexOf(":") >= 0) {
                        if (Objects.nonNull(sessions.get(rs.headers().get("uid")))) {
                            sessions.get(rs.headers().get("uid")).handlePublishMessage(publish, permitted -> {
                                PubRecMessage pubRec = new PubRecMessage();
                                pubRec.setMessageID(publish.getMessageID());
                                sendMessageToClient(pubRec);
                            });
                        } else
                            try {
                                writeLog(rs.headers().get("uid"), publish);
                            } catch (Exception e) {
                                logger.error(e.getMessage(), e);
                            }
                    } else {
                        String clientId = rs.headers().get("uid").length() == 13 ? "gw:" + rs.headers().get("uid")
                                : "app:" + rs.headers().get("uid");
                        if (Objects.nonNull(sessions.get(clientId))) {
                            sessions.get(clientId).handlePublishMessage(publish, permitted -> {
                                PubRecMessage pubRec = new PubRecMessage();
                                pubRec.setMessageID(publish.getMessageID());
                                sendMessageToClient(pubRec);
                            });
                        } else
                            try {
                                writeLog(clientId, publish);
                            } catch (Exception e) {
                                logger.error(e.getMessage(), e);
                            }
                    }
                }
                break;
            default:
                logger.error("qos not in (0,1,2) , value -> {}", rs.headers().get("qos"));
        }

    }

    /**
     * @Description 寫日志
     * @author zhang bo
     * @date 18-8-31
     * @version 1.0
     */
    public void writeLog(String clientId, PublishMessage publishMessage) throws Exception {
        DeliveryOptions opt = new DeliveryOptions().addHeader("tenant", clientId);

        publishMessage.setRetainFlag(false);
        Buffer msg = new MQTTEncoder().enc(publishMessage);
        if (publishMessage.getMessageID() != 0) {
            //持久化数据
            String[] arr = opt.getHeaders().get("tenant").split(":");
            JsonObject request = new JsonObject().put("msg", new JsonObject()
                    .put("message", publishMessage.getPayloadAsString())
                    .put("qos", QOSConvertUtils.toStr(publishMessage.getQos()))
                    .put("msgId", publishMessage.getMessageID())
                    .put("dst", arr[0]))
                    .put("msgId", publishMessage.getMessageID())
                    .put("topic", arr[1]).put("timeId", 0L);

            vertx.eventBus().send(LogAddr.class.getName() + WRITE_LOG, request, SendOptions.getInstance(), (AsyncResult<Message<Boolean>> rs) -> {
                if (rs.failed()) {
                    logger.error(rs.cause().getMessage(), rs.cause());
                }
            });
        }
    }


    public void sendMessageToClient(AbstractMessage message) {
        try {
            logger.debug(">>> " + message);
            Buffer b1 = encoder.enc(message);
            sendMessageToClient(b1);
        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void handleDisconnect(DisconnectMessage disconnectMessage) {
        removeCurrentSessionFromMap();
        session.handleDisconnect(disconnectMessage);
        session = null;
    }

    private void removeCurrentSessionFromMap() {
        String cid = session.getClientID();
        if (sessions != null && session.isCleanSession() && sessions.containsKey(cid)) {
            sessions.remove(cid);
        }
    }


    protected String getClientInfo() {
        String clientInfo = "Session n/a";
        if (session != null) {
            clientInfo = session.getClientInfo();
        }
        return clientInfo;
    }

    protected void handleWillMessage() {
//        logger.info("handle will message... ");
        if (session != null) {
//            logger.info("handle will message: session found!");
            session.handleWillMessage();
        }
//        logger.info("handle will message end.");
    }


    private void getloginAll() {
        vertx.eventBus().consumer("cn.login.all", msg -> {
            msg.reply(new JsonArray(sessions.keySet().stream().collect(Collectors.toList())));
        });
    }

}
