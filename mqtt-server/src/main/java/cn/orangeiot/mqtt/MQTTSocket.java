package cn.orangeiot.mqtt;

import cn.orangeiot.common.options.SendOptions;
import cn.orangeiot.mqtt.parser.MQTTDecoder;
import cn.orangeiot.mqtt.parser.MQTTEncoder;
import cn.orangeiot.mqtt.prometheus.PromMetrics;
import cn.orangeiot.mqtt.util.LogFileUtils;
import cn.orangeiot.reg.EventbusAddr;
import cn.orangeiot.reg.gateway.GatewayAddr;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.Message;
import io.vertx.core.impl.NetSocketInternal;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetSocket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dna.mqtt.moquette.proto.messages.*;

import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import static org.dna.mqtt.moquette.proto.messages.AbstractMessage.*;
import static org.dna.mqtt.moquette.proto.messages.AbstractMessage.QOSType.LEAST_ONE;

/**
 * Created by giovanni on 07/05/2014.
 * Base class for connection handling, 1 tcp connection corresponds to 1 instance of this class.
 */
public abstract class MQTTSocket implements MQTTPacketTokenizer.MqttTokenizerListener, Handler<Buffer>, EventbusAddr {

    private static Logger logger = LogManager.getLogger(MQTTSocket.class);

//    private final int DEFAULT_SENDMSG_TIMES = 5;//发送次数

    protected Vertx vertx;
    private MQTTDecoder decoder;
    private MQTTEncoder encoder;
    private MQTTPacketTokenizer tokenizer;
    protected MQTTSession session;
    private ConfigParser config;
    private Map<String, MQTTSession> sessions;
    private NetSocket netSocket;
    //    private int sendTimes;
    private final String USER_PREFIX = "app:";//用戶前綴
    private final String GATEWAY_PREFIX = "gw:";//网关前綴
    private final String GATEWAY_ON_OFF_STATE = "gatewayState";//網關狀態
    private final String REPLY_MESSAGE = "/clientId/rpc/reply";
    private ChannelHandlerContext chctx;
    private int timeout;
    private LogFileUtils logFileUtils;


    public MQTTSocket(int timeout, NetSocketInternal soi, Vertx vertx, ConfigParser config, Map<String, MQTTSession> sessions, NetSocket netSocket, LogFileUtils logFileUtils) {
        this.decoder = new MQTTDecoder();
        this.encoder = new MQTTEncoder();
        this.tokenizer = new MQTTPacketTokenizer();
        this.tokenizer.registerListener(this);
        this.vertx = vertx;
        this.config = config;
        this.sessions = sessions;
        this.netSocket = netSocket;
//        this.sendTimes = DEFAULT_SENDMSG_TIMES;
        this.chctx = soi.channelHandlerContext();
        this.timeout = timeout;
        this.logFileUtils = logFileUtils;
    }

    public MQTTSocket(int timeout, NetSocketInternal soi, Vertx vertx, ConfigParser config, Map<String, MQTTSession> sessions) {
        this.decoder = new MQTTDecoder();
        this.encoder = new MQTTEncoder();
        this.tokenizer = new MQTTPacketTokenizer();
        this.tokenizer.registerListener(this);
        this.vertx = vertx;
        this.config = config;
        this.sessions = sessions;
//        this.sendTimes = DEFAULT_SENDMSG_TIMES;
        this.chctx = soi.channelHandlerContext();
        this.timeout = timeout;
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
            } else {
                logger.warn("Timeout occurred ...");
            }
        } catch (Throwable ex) {
            String clientInfo = getClientInfo();
            logger.error(clientInfo + ", Bad error in processing the message", ex);
            logger.debug("client send message  content,client -> {}, content -> {}", clientInfo, new String(token));
            closeConnection();
            if (this.session != null) {
                this.session.closeState();
                this.session.release();
            }
            shutdown();
        }
    }

    @Override
    public void onError(Throwable e) {
        String clientInfo = getClientInfo();
        logger.error(clientInfo + ", " + e.getMessage(), e);
//        if(e instanceof CorruptedFrameException) {
        closeConnection();
        if (this.session != null) {
            this.session.closeState();
            this.session.release();
        }
        shutdown();
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
                this.session = new MQTTSession(vertx, config, netSocket, connectedClientID);
                this.session.setPublishMessageHandler(this::sendMessageToClient);
                this.session.handleConnectMessage(connect, authenticated -> {
                    if (authenticated.getBoolean("state")) {
                        connAck.setReturnCode(ConnAckMessage.CONNECTION_ACCEPTED);
                        if (this.session != null) {
                            this.session.setState(true);
                            this.session.createInitProcessInstance();//創建init實例
                        }
                        ;//有效狀態
                        if (checkConnected(this.session)) {
                            MQTTSession currentSession = sessions.putIfAbsent(connectedClientID, this.session);
                            if (Objects.nonNull(currentSession)) { //踢掉上個用戶
                                currentSession.shutdown();
                                currentSession.closeConnect();
                                currentSession.release();//釋放資源
                                sessions.remove(connectedClientID, currentSession);
                                sessions.putIfAbsent(connectedClientID, this.session);
                            }
                            logFileUtils.remove(connectedClientID);//移除离线实例
                            sendMessageToClient(connAck);
                            PromMetrics.mqtt_sessions_total.inc();
                            if (!session.isCleanSession()) {
//                            session.sendAllMessagesFromQueue();
                            }
                            AddheartIdle(connect);
                        }
                    } else {
                        logger.warn("Authentication failed! clientID= " + connect.getClientID() + " username=" + connect.getUsername());
//                        closeConnection();
                        if (Objects.nonNull(authenticated.getValue("header"))) {
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
                if (checkConnected(session)) {
//                    session.resetKeepAliveTimer();

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
                        if (session != null)
                            session.createPartitionLog(res -> {
                                if (res.failed()) {
                                    logger.error(res.cause().getMessage());
                                    subAck.addType(QOSType.FAILURE);
                                    sendMessageToClient(subAck);
                                } else {
                                    if (res.result()) {
                                        sendMessageToClient(subAck);
                                        if (session != null) session.processOfflineLog();
                                    } else {
                                        subAck.addType(QOSType.FAILURE);
                                        sendMessageToClient(subAck);
                                    }
                                }
                            });

                        if (session != null) {
                            checkDevice(session.getClientID(), "online");//在線狀態
                            checkUser(session.getClientID(), "online");//在線狀態
                        }
                    });
                }
                break;
            case UNSUBSCRIBE:
                if (checkConnected(session)) {
                    PromMetrics.mqtt_unsubscribe_total.labels(session.getClientID()).inc();

                    UnsubscribeMessage unsubscribeMessage = (UnsubscribeMessage) msg;
                    session.handleUnsubscribeMessage(unsubscribeMessage);
                    UnsubAckMessage unsubAck = new UnsubAckMessage();
                    unsubAck.setMessageID(unsubscribeMessage.getMessageID());
                    sendMessageToClient(unsubAck);
                }
                break;
            case PUBLISH:
                if (checkConnected(session)) {
                    PublishMessage publish = (PublishMessage) msg;

                    logger.debug("client publish topic -> {}", publish.getTopicName());

                    session.handlerPublishMessage(publish, session.getClientID(), rs -> {
                        if (rs.failed()) {
                            logger.error(rs.cause().getMessage());
                            publishReplyPackage(publish);
                        } else {
                            if (Objects.nonNull(rs.result()))
                                publishMsg(publish, rs.result());
                            else
                                logger.warn("reply data is null");
                        }
                    });
                }
                break;
            case PUBREC:
                if (checkConnected(session)) {
                    PubRecMessage pubRec = (PubRecMessage) msg;
                    PubRelMessage prelResp = new PubRelMessage();
                    prelResp.setMessageID(pubRec.getMessageID());
                    prelResp.setQos(LEAST_ONE);
                    sendMessageToClient(prelResp);
                }

                break;
            case PUBREL:
                if (checkConnected(session)) {
                    PubRelMessage pubRel = (PubRelMessage) msg;
                    PubCompMessage pubComp = new PubCompMessage();
                    pubComp.setMessageID(pubRel.getMessageID());
                    sendMessageToClient(pubComp);
                }
                break;
            case PUBACK:
                if (checkConnected(session)) {
                    session.getMessageFromQueue();
                    PubAckMessage pubAckMessage = (PubAckMessage) msg;

                    //确认到达
                    session.consumLog(pubAckMessage.getMessageID(), res -> {
                        if (res.failed()) {
                            logger.error(res.cause().getMessage(), res);
                        } else {
                            if (Objects.nonNull(res.result()) && vertx != null && res.result() != 0) {
                                vertx.cancelTimer(res.result());
                            } else {
                                logger.warn("receive ACK package , msgId timerId is null , clienid-> {} , AckMsgId -> {}"
                                        , session != null ? session.getClientID() : "empty", pubAckMessage.getMessageID());
                            }
                        }
                    });
                }
                break;
            case PUBCOMP:
                if (checkConnected(session)) {
                    session.getMessageFromQueue();

                    PubCompMessage pubCompMessage = (PubCompMessage) msg;

                    //确认到达
                    session.consumLog(pubCompMessage.getMessageID(), res -> {
                        if (res.failed()) {
                            logger.error(res.cause().getMessage(), res);
                        } else {
                            if (Objects.nonNull(res.result()) && vertx != null && res.result() != 0) {
                                vertx.cancelTimer(res.result());
                            } else {
                                logger.warn("receive ACK package , msgId timerId is null , clienid-> {} , AckMsgId -> {}"
                                        , session != null ? session.getClientID() : "empty", pubCompMessage.getMessageID());
                            }
                        }
                    });
                }
                break;
            case PINGREQ:
                if (checkConnected(session)) {
//                    session.resetKeepAliveTimer();
                    PingRespMessage pingResp = new PingRespMessage();
                    sendMessageToClient(pingResp);
                }
                break;
            case DISCONNECT:
                if (checkConnected(session)) {
                    PromMetrics.mqtt_disconnect_total.labels(session.getClientID()).inc();
                    if (session != null) {
                        checkDevice(session.getClientID(), "offline");//离线狀態
                        session.closeState();
                        session.release();
                    }
                    DisconnectMessage disconnectMessage = (DisconnectMessage) msg;
                    handleDisconnect(disconnectMessage);
                }
                break;
            default:
                checkConnected(session);
                logger.warn("type of message not known: " + msg.getClass().getSimpleName());
                break;
        }

    }


    /**
     * @Description 添加心跳处理
     * @author zhang bo
     * @date 18-9-29
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void AddheartIdle(ConnectMessage msg) {
        logger.debug("add idle state handle,msg ->{}", msg.getClientID());

        //移移除默認idle handler
        if (this.chctx.pipeline().get("idle") != null) {
            this.chctx.pipeline().remove("idle");
        }
        int keepAlive = 0;
        // client端保活時間爲0,前置最大時間
        if (msg.getKeepAlive() != 0) {
            // Idle 時間間隔 1.5 倍
            keepAlive = msg.getKeepAlive() +
                    msg.getKeepAlive() / 2;
        } else {
            keepAlive = this.timeout;
        }
        // 添加 channel pipeline idle handler
        if (this.chctx != null && this.chctx.pipeline().get("handler") != null) {
            this.chctx.pipeline().addBefore("handler", "idle", new IdleStateHandler(keepAlive, 0, 0));
            this.chctx.pipeline().addBefore("handler", "keepAliveHandler", new ChannelDuplexHandler() {
                @Override
                public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
                    if (evt instanceof IdleStateEvent) {
                        IdleStateEvent e = (IdleStateEvent) evt;
                        if (e.state() == IdleState.READER_IDLE) {
                            netSocket.close();
                        }
                    }
                }
            });
        }
    }

    /**
     * @Description 检查连接
     * @author zhang bo
     * @date 18-9-26
     * @version 1.0
     */
    private boolean checkConnected(MQTTSession session) {
        if (Objects.nonNull(session) && session.isState()) {
            return true;
        } else {
            session.closeState();
            session.release();
            shutdown();
            if (this.netSocket != null)
                this.netSocket.close();
            logger.warn("session is null , from message");
            return false;
        }
    }

    /**
     * @Description
     * @author zhang bo
     * @date 18-9-10
     * @version 1.0
     */
    public void checkDevice(String clientId, String state) {
        if (Objects.nonNull(clientId) && clientId.indexOf(GATEWAY_PREFIX) >= 0 && vertx != null) {//網關
            logger.debug("gwID -> {}", clientId);
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

    /**
     * @Description 检查用户
     * @author zhang bo
     * @date 18-9-29
     * @version 1.0
     */
    public void checkUser(String clientId, String state) {
        if (Objects.nonNull(clientId) && clientId.indexOf(USER_PREFIX) >= 0 && vertx != null) {//網關
            logger.debug("userID -> {}", clientId);
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
     * @Description 回复确认包
     * @author zhang bo
     * @date 18-6-29
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void publishReplyPackage(PublishMessage publish) {
        switch (publish.getQos()) {
            case RESERVED:
                session.handlePublishMessage(publish, null, false, false);
                break;
            case MOST_ONE:
                session.handlePublishMessage(publish, null, false, false);
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


    public void publishMsg(PublishMessage publish, JsonObject rs) {
        if (Objects.nonNull(rs) && this.session != null) {
            String tempTopic = publish.getTopicName();
            QOSType qos = publish.getQos();
            if (Objects.nonNull(rs.getValue("topicName")))
                publish.setTopicName(rs.getString("topicName"));
            JsonObject payload = rs;
            payload.remove("clientId");
            payload.remove("topicName");
            if (Objects.nonNull(rs))
                try {
                    publish.setPayload(rs.toString());
                } catch (UnsupportedEncodingException e) {
                    logger.error("add payload error , topicName -> {} , clientId -> {}" + publish.getTopicName(),
                            Objects.nonNull(session) ? session.getClientID() : "null");
                }
            String topic = Objects.nonNull(publish.getTopicName()) ? publish.getTopicName() : tempTopic;
            PromMetrics.mqtt_publish_total.labels(session.getClientID(), qos.name(), topic).inc();
            switch (publish.getQos()) {
                case RESERVED:
                    session.handlePublishMessage(publish, null, false, false);
                    break;
                case MOST_ONE:
                    session.handlePublishMessage(publish, null, false, false);
                    break;
                case LEAST_ONE:
                    session.handlePublishMessage(publish, permitted -> {
                        PubAckMessage pubAck = new PubAckMessage();
                        pubAck.setMessageID(publish.getMessageID());
                        sendMessageToClient(pubAck);
                    }, true, true);
                    break;
                case EXACTLY_ONCE:
                    session.handlePublishMessage(publish, permitted -> {
                        PubRecMessage pubRec = new PubRecMessage();
                        pubRec.setMessageID(publish.getMessageID());
                        sendMessageToClient(pubRec);
                    }, true, true);
                    break;
            }
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
//        if (sessions != null && session.isCleanSession() && sessions.containsKey(cid)) {
        if (sessions != null) {
            sessions.remove(cid, session);
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


//    private void getloginAll() {
//        vertx.eventBus().consumer("cn.login.all", msg -> {
//            msg.reply(new JsonArray(sessions.keySet().stream().collect(Collectors.toList())));
//        });
//    }


}
