package cn.orangeiot.mqtt;

import cn.orangeiot.common.options.SendOptions;
import cn.orangeiot.mqtt.log.handler.LogService;
import cn.orangeiot.mqtt.log.handler.impl.LogServiceImpl;
import cn.orangeiot.mqtt.parser.MQTTDecoder;
import cn.orangeiot.mqtt.parser.MQTTEncoder;
import cn.orangeiot.mqtt.persistence.StoreManager;
import cn.orangeiot.mqtt.persistence.Subscription;
import cn.orangeiot.mqtt.security.AuthorizationClient;
import cn.orangeiot.mqtt.util.QOSConvertUtils;
import cn.orangeiot.reg.EventbusAddr;
import cn.orangeiot.reg.log.LogAddr;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetSocket;
import org.apache.commons.collections4.map.PassiveExpiringMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dna.mqtt.moquette.proto.messages.*;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Giovanni Baleani on 07/05/2014.
 * Base class for connection handling, 1 tcp connection corresponds to 1 instance of this class.
 */
public class MQTTSession implements Handler<Message<Buffer>>, EventbusAddr {

    private static Logger logger = LogManager.getLogger(MQTTSession.class);

    public static final String ADDRESS = "io.github.giovibal.mqtt";
    public static final String TENANT_HEADER = "tenant";

    private final int DEFAULT_SENDMSG_TIMES = 2;//发送次数

    private Vertx vertx;
    private MQTTDecoder decoder;
    private MQTTEncoder encoder;
    private ITopicsManager topicsManager;
    private String clientID;
    private String protoName;
    private boolean cleanSession;
    private String tenant;
    private boolean securityEnabled;
    private String authenticatorAddress;
    private String authorizationToken = null;
    private boolean retainSupport;
    private MessageConsumer<Buffer> messageConsumer;
    private Handler<PublishMessage> publishMessageHandler;
    private Map<String, Subscription> subscriptions;
    private QOSUtils qosUtils;
    private StoreManager storeManager;
    private Map<String, List<Subscription>> matchingSubscriptionsCache;
    private PublishMessage willMessage;
    private String publish;

    private NetSocket netSocket;
    private int sendTimes;
    private String user_prefix;//用戶前綴
    private String gateway_prefix;//网关前綴
    private LogService logService;//log服务

    public static Map<String, Subscription> suscribeMap = new ConcurrentHashMap<>();

    private Queue<PublishMessage> queue;

    public MQTTSession(Vertx vertx, ConfigParser config, NetSocket netSocket, String clientID) {
        this.vertx = vertx;
        this.decoder = new MQTTDecoder();
        this.encoder = new MQTTEncoder();
        this.securityEnabled = config.isSecurityEnabled();
        this.retainSupport = config.isRetainSupport();
        this.subscriptions = new LinkedHashMap<>();
        this.qosUtils = new QOSUtils();
        this.publish = config.getPublish();
        this.netSocket = netSocket;
        this.sendTimes = DEFAULT_SENDMSG_TIMES;
        this.clientID = clientID;
        this.user_prefix = config.getUser_prefix();
        this.gateway_prefix = config.getGateway_prefix();
        logService = new LogServiceImpl(config.getDirPath(), vertx, clientID, config.getSegmentSize(), config.getExpireTime());

        PassiveExpiringMap.ConstantTimeToLiveExpirationPolicy<String, List<Subscription>>
                expirePeriod = new PassiveExpiringMap.ConstantTimeToLiveExpirationPolicy<>(
                30, TimeUnit.MINUTES);
        this.matchingSubscriptionsCache = new PassiveExpiringMap<>(expirePeriod, new HashMap<>());

        this.topicsManager = new MQTTTopicsManagerOptimized();
        this.storeManager = new StoreManager(this.vertx);
        this.authenticatorAddress = config.getAuthenticatorAddress();

        this.queue = new LinkedList<>();
    }

    /**
     * @Description 斷開連接
     * @author zhang bo
     * @date 18-6-29
     * @version 1.0
     */
    public void closeConnect() {
        if (Objects.nonNull(netSocket))
            netSocket.close();
    }


    public LogService getLogService() {
        return logService;
    }

    public void addMessageToQueue(PublishMessage pm) {
        queue.add(pm);
    }

    public PublishMessage getMessageFromQueue() {
        return queue.poll();
    }

    public void sendAllMessagesFromQueue() {
        PublishMessage queuedMessage;
        while ((queuedMessage = getMessageFromQueue()) != null) {
            sendPublishMessage(queuedMessage);
        }
    }

    private String extractTenant(String username) {
        if (username == null || username.trim().length() == 0)
            return "";
        String tenant = "";
        int idx = username.lastIndexOf('@');
        if (idx > 0) {
            tenant = username.substring(idx + 1);
        }
        return tenant;
    }

    public void setPublishMessageHandler(Handler<PublishMessage> publishMessageHandler) {
        this.publishMessageHandler = publishMessageHandler;
    }

    public PublishMessage getWillMessage() {
        return willMessage;
    }

    public void handleConnectMessage(ConnectMessage connectMessage,
                                     Handler<JsonObject> authHandler) throws Exception {

        clientID = connectMessage.getClientID();

        if (verifyClientId(clientID)) {
            cleanSession = connectMessage.isCleanSession();
            protoName = connectMessage.getProtocolName();
            if ("MQIsdp".equals(protoName)) {
                logger.debug("Detected MQTT v. 3.1 " + protoName + ", clientID: " + clientID);
            } else if ("MQTT".equals(protoName)) {
                logger.debug("Detected MQTT v. 3.1.1 " + protoName + ", clientID: " + clientID);
            } else {
                logger.debug("Detected MQTT protocol " + protoName + ", clientID: " + clientID);
            }

            String username = connectMessage.getUsername();
            String password = connectMessage.getPassword();

            if (securityEnabled) {
                AuthorizationClient auth = new AuthorizationClient(vertx.eventBus(), authenticatorAddress);
                auth.authorize(username, password, getClientID(), validationInfo -> {
                    if (validationInfo.auth_valid) {
                        authorizationToken = validationInfo.token;
                        String tenant = validationInfo.tenant;
                        _initTenant(tenant);
                        _handleConnectMessage(connectMessage);
                        authHandler.handle(new JsonObject().put("state", Boolean.TRUE));
                    } else {
                        JsonObject json = new JsonObject().put("state", Boolean.FALSE);
                        if (Objects.nonNull(validationInfo.getHeader()))
                            json.put("header", validationInfo.getHeader());
                        authHandler.handle(json);
                    }
                });
            } else {
                String clientID = connectMessage.getClientID();
                String tenant = null;
                if (username == null || username.trim().length() == 0) {
                    tenant = extractTenant(clientID);
                } else {
                    tenant = extractTenant(username);
                }
                _initTenant(tenant);
                _handleConnectMessage(connectMessage);
                authHandler.handle(new JsonObject().put("state", Boolean.TRUE));
            }
        } else
            authHandler.handle(new JsonObject().put("state", Boolean.FALSE));
    }


    /**
     * @param clientId client端 連接的唯一標識
     * @Description 檢驗client是否合法
     * @author zhang bo
     * @date 18-9-30
     * @version 1.0
     */
    public boolean verifyClientId(String clientId) {
        if (StringUtils.isNotBlank(clientId)) {
            String[] arrs = clientId.split(":");
            if (arrs.length == 2 && (arrs[0].equals(user_prefix) || arrs[0].equals(gateway_prefix))) {
                return true;
            } else
                return false;
        } else {
            return false;
        }
    }

    private void _initTenant(String tenant) {
        if (tenant == null)
            throw new IllegalStateException("Tenant cannot be null");
        this.tenant = tenant;
    }

    private void _handleConnectMessage(ConnectMessage connectMessage) {
        if (!cleanSession) {
            logger.debug("cleanSession=false: restore old session state with subscriptions ...");
            /*
            1. check if a prior session is present then restore; if not: create new session and persist it
            2. retrieve all subscriptions from session, and resubscribe to all
            3. resent all qos 1,2 messages not "acknowledged"
             */

        } else {
            boolean isWillFlag = connectMessage.isWillFlag();
            if (isWillFlag) {
                String willMessageM = connectMessage.getWillMessage();
                String willTopic = connectMessage.getWillTopic();
                byte willQosByte = connectMessage.getWillQos();
                AbstractMessage.QOSType willQos = qosUtils.toQos(willQosByte);

                try {
                    willMessage = new PublishMessage();
                    willMessage.setPayload(willMessageM);
                    willMessage.setTopicName(willTopic);
                    willMessage.setQos(willQos);
                    switch (willQos) {
                        case EXACTLY_ONCE:
                        case LEAST_ONE:
                            willMessage.setMessageID(1);
                    }
                } catch (UnsupportedEncodingException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
//        startKeepAliveTimer(connectMessage.getKeepAlive());
        logger.debug("New connection client : " + getClientInfo());
    }

    /**
     * @param persistenceFlag  是否持久化
     * @param retryflag        是否重發
     * @param publishMessage   消息体
     * @param completedHandler future函数
     * @Description 发送消息
     * @author zhang bo
     * @date 18-10-28
     * @version 1.0
     */
    public void handlePublishMessage(PublishMessage publishMessage, Handler<Boolean> completedHandler, boolean persistenceFlag, boolean retryflag) {
        if (authorizationToken != null && vertx != null) {
            AuthorizationClient auth = new AuthorizationClient(vertx.eventBus(), authenticatorAddress);
            auth.authorizePublish(authorizationToken, publishMessage.getTopicName(), permitted -> {
                if (permitted) {
                    _handlePublishMessage(publishMessage, persistenceFlag, retryflag);
                }
                if (completedHandler != null) completedHandler.handle(permitted);
            });
        } else {
            _handlePublishMessage(publishMessage, persistenceFlag, retryflag);
            if (completedHandler != null) completedHandler.handle(Boolean.TRUE);
        }
    }

    private void _handlePublishMessage(PublishMessage publishMessage, boolean persistenceFlag, boolean retryflag) {
        try {
            // publish always have tenant, if session is not tenantized, tenant is retrieved from topic ([tenant]/to/pi/c)
            String publishTenant = calculatePublishTenant(publishMessage.getTopicName());

            // store retained messages ...
            if (publishMessage.isRetainFlag()) {
                boolean payloadIsEmpty = false;
                ByteBuffer bb = publishMessage.getPayload();
                if (bb != null) {
                    byte[] bytes = bb.array();
                    if (bytes.length == 0) {
                        payloadIsEmpty = true;
                    }
                }
                if (payloadIsEmpty) {
                    storeManager.deleteRetainMessage(publishTenant, publishMessage.getTopicName());
                } else {
                    storeManager.saveRetainMessage(publishTenant, publishMessage);
                }
            }

            /* It MUST set the RETAIN flag to 0 when a PUBLISH Packet is sent to a Client
             * because it matches an established subscription
             * regardless of how the flag was set in the message it received. */
            publishMessage.setRetainFlag(false);
//            Buffer msg = encoder.enc(publishMessage);
            if (tenant == null)
                tenant = "";

            String clientId = getClienId(publishMessage.getTopicName());
            DeliveryOptions opt = new DeliveryOptions().addHeader(TENANT_HEADER, clientId);

            if (!Objects.nonNull(publishMessage.getMessageID()))
                publishMessage.setMessageID(0);

            if (publishMessage.getMessageID() != 0 && persistenceFlag) {
                defaultWriteLogRetry(publishMessage, retryflag, rs -> {
                    if (rs.failed()) {
                        logger.error(rs.cause().getMessage(), rs);
                    } else {
                        if (rs.result()) {
//                            vertx.eventBus().publish(ADDRESS, msg, opt);
                            handlePublishMessageReceived(publishMessage);
                        }
                    }
                });
            } else {
                handlePublishMessageReceived(publishMessage);
            }
        } catch (Throwable e) {
            e.printStackTrace();
            logger.error(e.getMessage());
        }
    }


    /**
     * @Description
     * @author zhang bo
     * @date 18-8-8
     * @version 1.0
     */
    public String getClienId(String topic) {
        String clientId = "";
        if (Objects.nonNull(topic)) {
            String arr[] = topic.split("/");
            if (topic.indexOf("/orangeiot") >= 0 && topic.indexOf("/call") >= 0) {
                clientId = "gw:" + arr[2];
            } else if (topic.indexOf("/orangeiot") >= 0 && topic.indexOf("/reply") >= 0) {
                clientId = "app:" + arr[1];
            } else {//user
                if (topic.indexOf("/request/app/func") < 0) {
                    clientId = "app:" + arr[1];
                } else {
                    clientId = clientID;
                }
            }
        }
        return clientId;
    }


    /**
     * @Description 写入log
     * @author zhang bo
     * @date 18-7-30
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    @Deprecated
    public void writeLog(PublishMessage publishMessage, DeliveryOptions opt, Buffer msg, Handler<AsyncResult<Boolean>> handler) {
        //周期发送
        AtomicInteger atomicInteger = new AtomicInteger(0);
        Long timeId = vertx.setPeriodic(2000, id -> {
            logger.debug("periodic send -> " + msg);
            vertx.eventBus().publish(ADDRESS, msg, opt);
            atomicInteger.incrementAndGet();
            if (atomicInteger.get() == sendTimes) {
                vertx.cancelTimer(id);
            }
        });
        //持久化数据
        String[] arr = opt.getHeaders().get(TENANT_HEADER).split(":");
        JsonObject request = new JsonObject().put("msg", new JsonObject()
                .put("message", publishMessage.getPayloadAsString())
                .put("qos", QOSConvertUtils.toStr(publishMessage.getQos()))
                .put("msgId", publishMessage.getMessageID())
                .put("dst", arr[0]))
                .put("msgId", publishMessage.getMessageID())
                .put("topic", arr[1]).put("timeId", timeId);

        vertx.eventBus().send(LogAddr.class.getName() + WRITE_LOG, request, SendOptions.getInstance(), (AsyncResult<Message<Boolean>> rs) -> {
            if (rs.failed()) {
                handler.handle(Future.failedFuture(rs.cause()));
                vertx.cancelTimer(timeId);
            } else {
                handler.handle(Future.succeededFuture(rs.result().body()));
            }
        });
    }


    /**
     * @Description 延遲重試
     * @author zhang bo
     * @date 18-11-6
     * @version 1.0
     */
    public long delayRetry(PublishMessage publishMessage) {
        long timerId = vertx.setTimer(2000, id -> {
            logger.debug("timer send -> {} , timeId -> {}", publishMessage.getTopicName(), id);
            handlePublishMessageReceived(publishMessage);
        });
        return timerId;
    }


    /**
     * @param publishMessage 消息体
     * @param retryFlag      是否重發
     * @Description 写入log和重發機制  重發次數 1次
     * @author zhang bo
     * @date 18-10-23
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void defaultWriteLogRetry(PublishMessage publishMessage, boolean retryFlag, Handler<AsyncResult<Boolean>> handler) {
        logger.debug("defaultWriteLogRetry , clientId -> {} , msgId -> {}", clientID, publishMessage.getMessageID());
        //周期发送
//        AtomicInteger atomicInteger = new AtomicInteger(0);
//        Long timeId = vertx.setPeriodic(2000, id -> {
//            logger.debug("periodic send -> " + publishMessage.getTopicName());
////            vertx.eventBus().publish(ADDRESS, msg, opt);
//            handlePublishMessageReceived(publishMessage);
//            atomicInteger.incrementAndGet();
//            if (atomicInteger.get() == sendTimes && vertx != null) {
//                vertx.cancelTimer(id);
//            }
//        });
        if (vertx != null) {
            long timeId;
            if (retryFlag)
                timeId = delayRetry(publishMessage);
            else
                timeId = 0;
            logService.writeLog(publishMessage.getPayloadAsString(), publishMessage.getMessageID(), timeId,
                    QOSConvertUtils.toByte(publishMessage.getQos()), res -> {
                        if (res.failed()) {
                            handler.handle(res);
                            if (vertx != null) vertx.cancelTimer(timeId);
                        } else {
                            handler.handle(res);
                        }
                    });
        }
    }

    /**
     * @param publishMessage 消息体
     * @param opt            附加信息体
     * @Description 重試機制
     * @author zhang bo
     * @date 18-10-30
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void defaultRetryPublish(PublishMessage publishMessage, DeliveryOptions opt, Handler<AsyncResult<Boolean>> handler) {
        logger.debug("defaultRetryPublish , clientId -> {} , msgId -> {}", clientID, publishMessage.getMessageID());
        //周期发送
        AtomicInteger atomicInteger = new AtomicInteger(0);
        Long timeId = vertx.setPeriodic(2000, id -> {
            logger.debug("periodic send -> " + publishMessage.getTopicName());
//            vertx.eventBus().publish(ADDRESS, msg, opt);
            handlePublishMessageReceived(publishMessage);
            atomicInteger.incrementAndGet();
            if (atomicInteger.get() == sendTimes) {
                vertx.cancelTimer(id);
            }
        });
        logService.updateTimerId(publishMessage.getMessageID(), timeId, res -> {
            if (res.failed()) {
                handler.handle(res);
                vertx.cancelTimer(timeId);
            } else {
                handler.handle(res);
            }
        });

    }


    private String calculatePublishTenant(String topic) {
        boolean isTenantSession = isTenantSession();
        if (isTenantSession) {
            return tenant;
        } else {
            String t;
            boolean slashFirst = topic.startsWith("/");
            int idx_start = 0;
            int idx_end = -1;
            if (slashFirst) {
                idx_start = 1;
            } else {
                idx_start = 0;
            }

            idx_end = topic.indexOf('/', idx_start);
            if (idx_end > 1 && idx_end > idx_start) {
                t = topic.substring(idx_start, idx_end);
            } else {
//                t = topic.substring(idx_start);
                t = ""; // global tenant...
            }

            return t;
        }
    }

    public void handleSubscribeMessage(SubscribeMessage subscribeMessage, Handler<JsonArray> completedHandler) {
        if (authorizationToken != null && vertx != null) {
            AuthorizationClient auth = new AuthorizationClient(vertx.eventBus(), authenticatorAddress);
            auth.authorizeSubscribe(authorizationToken, subscribeMessage.subscriptions(), permitted -> {
                _handleSubscribeMessage(subscribeMessage, permitted);
                if (completedHandler != null) completedHandler.handle(permitted);
            });
        } else {
            JsonArray permitted = new JsonArray();
            for (int i = 0; i < subscribeMessage.subscriptions().size(); i++) {
                permitted.add(true);
            }
            _handleSubscribeMessage(subscribeMessage, permitted);
            if (completedHandler != null) completedHandler.handle(permitted);
        }
    }

    private void _handleSubscribeMessage(SubscribeMessage subscribeMessage, JsonArray permitted) {
        try {
            final int messageID = subscribeMessage.getMessageID();
            if (this.messageConsumer == null && vertx != null) {
                messageConsumer = vertx.eventBus().consumer(ADDRESS);
                messageConsumer.handler(this);
            }

            // invalidate matching topic cache
            matchingSubscriptionsCache.clear();

            List<SubscribeMessage.Couple> subs = subscribeMessage.subscriptions();
            int indx = 0;
            for (SubscribeMessage.Couple s : subs) {
                if (permitted.getBoolean(indx++)) {
                    String topicFilter = s.getTopicFilter();
                    Subscription sub = new Subscription();
                    sub.setQos(s.getQos());
                    sub.setTopicFilter(topicFilter);
                    this.subscriptions.put(topicFilter, sub);

                    suscribeMap.put(topicFilter, sub);

                    String publishTenant = calculatePublishTenant(topicFilter);

                    // check in client wants receive retained message by this topicFilter
                    if (retainSupport) {
                        storeManager.getRetainedMessagesByTopicFilter(publishTenant, topicFilter, (List<PublishMessage> retainedMessages) -> {
                            if (retainedMessages != null) {
                                int incrMessageID = messageID;
                                for (PublishMessage retainedMessage : retainedMessages) {
                                    switch (retainedMessage.getQos()) {
                                        case LEAST_ONE:
                                        case EXACTLY_ONCE:
                                            retainedMessage.setMessageID(++incrMessageID);
                                    }
                                    retainedMessage.setRetainFlag(true);
                                    handlePublishMessageReceived(retainedMessage);
                                }
                            }
                        });
                    }
                }
            }
        } catch (Throwable e) {
            logger.error(e.getMessage());
        }
    }

    private boolean isTenantSession() {
        boolean isTenantSession = tenant != null && tenant.trim().length() > 0;
        return isTenantSession;
    }

    private boolean tenantMatch(Message<Buffer> message) {
        boolean isTenantSession = isTenantSession();
        boolean tenantMatch;
        if (isTenantSession) {
            boolean containsTenantHeader = message.headers().contains(TENANT_HEADER);
            if (containsTenantHeader) {
                String tenantHeaderValue = message.headers().get(TENANT_HEADER);
                tenantMatch =
                        tenant.equals(tenantHeaderValue)
                                || "".equals(tenantHeaderValue)
                ;
            } else {
                // if message doesn't contains header is not for a tenant-session
                tenantMatch = false;
            }
        } else {
            // if this is not a tenant-session, receive all messages from all tenants
            tenantMatch = true;
        }
        return tenantMatch;
    }

    @Override
    public void handle(Message<Buffer> message) {
        try {
            boolean tenantMatch = tenantMatch(message);
            if (tenantMatch) {
                Buffer in = message.body();
                PublishMessage pm = (PublishMessage) decoder.dec(in);
                // filter messages by of subscriptions of this client
                if (pm == null) {
                    logger.warn("PublishMessage is null, message.headers => " + message.headers().entries() + "");
                } else {
                    handlePublishMessageReceived(pm);
                }
            }
        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
        }
    }

    public void handlePublishMessageReceived(PublishMessage publishMessage) {
        boolean publishMessageToThisClient = false;
        int maxQos = -1;

        /*
         * the Server MUST deliver the message to the Client respecting the maximum QoS of all the matching subscriptions
         */
        String topic = publishMessage.getTopicName();
        List<Subscription> subs = getAllMatchingSubscriptions(topic);
        if (subs != null && subs.size() > 0) {
            publishMessageToThisClient = true;
            for (Subscription s : subs) {
                int itemQos = s.getQos();
                if (itemQos > maxQos) {
                    maxQos = itemQos;
                }
                // optimization: if qos==2 is alredy **the max** allowed
                if (maxQos == 2)
                    break;
            }
        }

        if (publishMessageToThisClient) {
            // the qos cannot be bigger than the subscribe requested qos ...
            AbstractMessage.QOSType originalQos = publishMessage.getQos();
            int iSentQos = qosUtils.toInt(originalQos);
            int iOkQos = qosUtils.calculatePublishQos(iSentQos, maxQos);
            AbstractMessage.QOSType qos = qosUtils.toQos(iOkQos);
            publishMessage.setQos(qos);
            if (!cleanSession && iSentQos > 0) {
                addMessageToQueue(publishMessage);
            }
            sendPublishMessage(publishMessage);
        }
    }

    private List<Subscription> getAllMatchingSubscriptions(String topic) {
        List<Subscription> ret = new ArrayList<>();
//        String topic = pm.getTopicName();
        if (matchingSubscriptionsCache.containsKey(topic)) {
            return matchingSubscriptionsCache.get(topic);
        }
        // check if topic of published message pass at least one of the subscriptions
        for (Subscription c : subscriptions.values()) {
            String topicFilter = c.getTopicFilter();
            boolean match = topicsManager.match(topic, topicFilter);
            if (match) {
                ret.add(c);
            }
        }
        matchingSubscriptionsCache.put(topic, ret);
        return ret;
    }

    private void sendPublishMessage(PublishMessage pm) {
        if (publishMessageHandler != null)
            publishMessageHandler.handle(pm);
    }


    public void handleUnsubscribeMessage(UnsubscribeMessage unsubscribeMessage) {
        try {
            List<String> topicFilterSet = unsubscribeMessage.topicFilters();
            for (String topicFilter : topicFilterSet) {
                if (subscriptions != null) {
                    subscriptions.remove(topicFilter);
                    matchingSubscriptionsCache.clear();

                    suscribeMap.remove(topicFilter);
                }
            }
        } catch (Throwable e) {
            logger.error(e.getMessage());
        }
    }

    /**
     * @Description 創建分區log
     * @author zhang bo
     * @date 18-10-23
     * @version 1.0
     */
    public void createPartitionLog(Handler<AsyncResult<Boolean>> handler) {
        logService.createPartitionLog(handler);
    }


    /**
     * @Description 創建分區log
     * @author zhang bo
     * @date 18-10-23
     * @version 1.0
     */
    public void consumLog(int msgId, Handler<AsyncResult<Long>> handler) {
        logService.consumLog(msgId, handler);
    }


    /**
     * @Description 處理離線消息
     * @author zhang bo
     * @date 18-10-23
     * @version 1.0
     */
    public void processOfflineLog() {
        logService.processOfflineMsg();
    }


    /**
     * @Description 釋放資源
     * @author zhang bo
     * @date 18-11-2
     * @version 1.0
     */
    public void release() {
        logService.release();
    }


    /**
     * @Description 關閉狀態
     * @author zhang bo
     * @date 18-10-30
     * @version 1.0
     */
    public void closeState() {
        logService.closeState();
    }


    /**
     * @Description publish处理
     * @author zhang bo
     * @date 17-12-11
     * @version 1.0
     */
    public void handlerPublishMessage(PublishMessage publishMessage, String clientId, Handler<AsyncResult<JsonObject>> asyncResultHandler) {
        logger.debug("Message payload from " + publishMessage.getPayloadAsString());
        DeliveryOptions deliveryOptions = new DeliveryOptions();
        deliveryOptions.addHeader("qos", QOSConvertUtils.toStr(publishMessage.getQos()));
        if (Objects.nonNull(publishMessage.getMessageID())) {
            deliveryOptions.addHeader("messageId", String.valueOf(publishMessage.getMessageID()));
        } else {
            deliveryOptions.addHeader("messageId", "0");
        }
        if (vertx != null)
            vertx.eventBus().send(publish, new JsonObject(publishMessage.getPayloadAsString())
                            .put("topicName", publishMessage.getTopicName()).put("clientId", clientId),
                    deliveryOptions.setSendTimeout(2000), (AsyncResult<Message<JsonObject>> rs) -> {
                        if (rs.failed()) {
                            asyncResultHandler.handle(Future.failedFuture(rs.cause().getMessage()));
                        } else {
                            if (Objects.nonNull(rs.result()) && Objects.nonNull(rs.result().body())) {
                                asyncResultHandler.handle(Future.succeededFuture(rs.result().body()));
                            } else {
                                asyncResultHandler.handle(Future.failedFuture("return data is null"));
                            }
                        }
                    });

    }

    public void handleDisconnect(DisconnectMessage disconnectMessage) {
        logger.debug("Disconnect from " + clientID + " ...");
        /*
         * : implement this behaviour
         * On receipt of DISCONNECT the Server:
         * - MUST discard any Will Message associated with the current connection without publishing it, as described in Section 3.1.2.5 [MQTT-3.14.4-3].
         * - SHOULD close the Network Connection if the Client has not already done so.
         */
        shutdown();
    }

    public void shutdown() {
        // stop timers
//        stopKeepAliveTimer();

        // deallocate this instance ...
        if (messageConsumer != null) {
            messageConsumer.unregister();
            messageConsumer = null;
            this.matchingSubscriptionsCache.clear();
            this.subscriptions.forEach((key, val) -> suscribeMap.remove(key, val));
        }
        vertx = null;
    }


    public void sendMessageToClient(AbstractMessage message) {
        try {
            logger.debug(">>> " + message);
            Buffer b1 = encoder.enc(message);
            netSocket.write(b1);
            if (netSocket.writeQueueFull()) {
                netSocket.pause();
                netSocket.drainHandler(done -> netSocket.resume());
            }
        } catch (Throwable e) {
            logger.error(e.getMessage());
        }
    }

    public void handleWillMessage() {
        // publish will message if present ...
        if (willMessage != null) {
            logger.debug("publish will message ... topic[" + willMessage.getTopicName() + "]");
            handlePublishMessage(willMessage, null, true, false);
        }
    }

    public String getClientInfo() {
        String clientInfo = "clientID: " + clientID + ", MQTT protocol: " + protoName + "";
        return clientInfo;
    }

    public String getClientID() {
        return this.clientID;
    }

    public MQTTSession setClientID(String clientID) {
        this.clientID = clientID;
        return this;
    }

    public String getProtoName() {
        return protoName;
    }

    public boolean isCleanSession() {
        return cleanSession;
    }
}
