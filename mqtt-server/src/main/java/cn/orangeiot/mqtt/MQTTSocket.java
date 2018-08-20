package cn.orangeiot.mqtt;

import cn.orangeiot.common.options.SendOptions;
import cn.orangeiot.mqtt.parser.MQTTEncoder;
import cn.orangeiot.mqtt.prometheus.PromMetrics;
import cn.orangeiot.mqtt.parser.MQTTDecoder;
import cn.orangeiot.reg.EventbusAddr;
import cn.orangeiot.reg.log.LogAddr;
import cn.orangeiot.reg.message.MessageAddr;
import cn.orangeiot.reg.storage.StorageAddr;
import io.vertx.core.AsyncResult;
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
import sun.rmi.runtime.Log;

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
                    logger.warn("Session alredy allocated ...");
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
                    logger.info("keep alive exausted! closing connection for client[" + cinfo + "] ...");
                    closeConnection();
                });
                session.handleConnectMessage(connect, authenticated -> {
                    if (authenticated) {
                        connAck.setReturnCode(ConnAckMessage.CONNECTION_ACCEPTED);
                        sessions.put(session.getClientID(), session);
                        sendMessageToClient(connAck);
                        if (!session.isCleanSession()) {
                            session.sendAllMessagesFromQueue();
                        }

                    } else {
                        logger.error("Authentication failed! clientID= " + connect.getClientID() + " username=" + connect.getUsername());
//                        closeConnection();
                        connAck.setReturnCode(ConnAckMessage.BAD_USERNAME_OR_PASSWORD);
                        sendMessageToClient(connAck);
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

                logger.info("==client publish topic:" + publish.getTopicName() + "==payload:" + publish.getPayloadAsString());

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
            QOSType qos = publish.getQos();
            publish.setTopicName(rs.getString("topicName"));
            if (flag)
                try {
                    rs.remove("topicName");
                    publish.setPayload(rs.toString());
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            String topic = publish.getTopicName();
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
                if (Objects.nonNull(session)) {
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
                if (Objects.nonNull(session)) {
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
                        sessions.get(rs.headers().get("uid")).handlePublishMessage(publish, permitted -> {
                            PubAckMessage pubAck = new PubAckMessage();
                            pubAck.setMessageID(publish.getMessageID());
                            sendMessageToClient(pubAck);
                        });
                    } else {
                        sessions.get(rs.headers().get("uid").length() == 13 ? "gw:" + rs.headers().get("uid")
                                : "app:" + rs.headers().get("uid")).handlePublishMessage(publish, permitted -> {
                            PubAckMessage pubAck = new PubAckMessage();
                            pubAck.setMessageID(publish.getMessageID());
                            sendMessageToClient(pubAck);
                        });
                    }
                }
                break;
            case 2:
                publish.setQos(EXACTLY_ONCE);
                if (Objects.nonNull(session)) {
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
                        sessions.get(rs.headers().get("uid")).handlePublishMessage(publish, permitted -> {
                            PubRecMessage pubRec = new PubRecMessage();
                            pubRec.setMessageID(publish.getMessageID());
                            sendMessageToClient(pubRec);
                        });
                    } else {
                        sessions.get(rs.headers().get("uid").length() == 13 ? "gw:" + rs.headers().get("uid")
                                : "app:" + rs.headers().get("uid")).handlePublishMessage(publish, permitted -> {
                            PubRecMessage pubRec = new PubRecMessage();
                            pubRec.setMessageID(publish.getMessageID());
                            sendMessageToClient(pubRec);
                        });
                    }
                }
                break;
            default:
                logger.error("qos not in (0,1,2) , value -> {}", rs.headers().get("qos"));
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
