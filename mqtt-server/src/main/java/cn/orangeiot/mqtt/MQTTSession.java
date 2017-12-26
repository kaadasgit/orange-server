package cn.orangeiot.mqtt;

import cn.orangeiot.mqtt.parser.MQTTDecoder;
import cn.orangeiot.mqtt.parser.MQTTEncoder;
import cn.orangeiot.mqtt.persistence.StoreManager;
import cn.orangeiot.mqtt.persistence.Subscription;
import cn.orangeiot.mqtt.security.AuthorizationClient;
import com.fasterxml.jackson.databind.util.JSONPObject;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.commons.collections4.map.PassiveExpiringMap;
import org.dna.mqtt.moquette.proto.messages.*;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Created by Giovanni Baleani on 07/05/2014.
 * Base class for connection handling, 1 tcp connection corresponds to 1 instance of this class.
 */
public class MQTTSession implements Handler<Message<Buffer>> {

    private static Logger logger = LoggerFactory.getLogger(MQTTSession.class);

    public static final String ADDRESS = "io.github.giovibal.mqtt";
    public static final String TENANT_HEADER = "tenant";

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

    private long keepAliveTimerID = -1;
    private boolean keepAliveTimeEnded;
    private Handler<String> keepaliveErrorHandler;

    public static Map<String, Subscription> suscribeMap=new ConcurrentHashMap<>();

    private Queue<PublishMessage> queue;

    public MQTTSession(Vertx vertx, ConfigParser config) {
        this.vertx = vertx;
        this.decoder = new MQTTDecoder();
        this.encoder = new MQTTEncoder();
        this.securityEnabled = config.isSecurityEnabled();
        this.retainSupport = config.isRetainSupport();
        this.subscriptions = new LinkedHashMap<>();
        this.qosUtils = new QOSUtils();
        this.publish=config.getPublish();

        PassiveExpiringMap.ConstantTimeToLiveExpirationPolicy<String, List<Subscription>>
                expirePeriod = new PassiveExpiringMap.ConstantTimeToLiveExpirationPolicy<>(
                30, TimeUnit.MINUTES);
        this.matchingSubscriptionsCache = new PassiveExpiringMap<>( expirePeriod, new HashMap<>() );

        this.topicsManager = new MQTTTopicsManagerOptimized();
        this.storeManager = new StoreManager(this.vertx);
        this.authenticatorAddress = config.getAuthenticatorAddress();
        
        this.queue = new LinkedList<>();
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
        if(username == null || username.trim().length()==0)
            return "";
        String tenant = "";
        int idx = username.lastIndexOf('@');
        if(idx > 0) {
            tenant = username.substring(idx+1);
        }
        return tenant;
    }

    public void setPublishMessageHandler(Handler<PublishMessage> publishMessageHandler) {
        this.publishMessageHandler = publishMessageHandler;
    }

    public void setKeepaliveErrorHandler(Handler<String> keepaliveErrorHandler) {
        this.keepaliveErrorHandler = keepaliveErrorHandler;
    }

    public PublishMessage getWillMessage() {
        return willMessage;
    }

    public void handleConnectMessage(ConnectMessage connectMessage,
                                     Handler<Boolean> authHandler)
            throws Exception {

        clientID = connectMessage.getClientID();
        cleanSession = connectMessage.isCleanSession();
        protoName = connectMessage.getProtocolName();
        if("MQIsdp".equals(protoName)) {
            logger.debug("Detected MQTT v. 3.1 " + protoName + ", clientID: " + clientID);
        } else if("MQTT".equals(protoName)) {
            logger.debug("Detected MQTT v. 3.1.1 " + protoName + ", clientID: " + clientID);
        } else {
            logger.debug("Detected MQTT protocol " + protoName + ", clientID: " + clientID);
        }

        String username = connectMessage.getUsername();
        String password = connectMessage.getPassword();

        if(securityEnabled) {
            AuthorizationClient auth = new AuthorizationClient(vertx.eventBus(), authenticatorAddress);
            auth.authorize(username, password,getClientID(), validationInfo -> {
                if (validationInfo.auth_valid) {
                	authorizationToken = validationInfo.token;
                    String tenant = validationInfo.tenant;
                    _initTenant(tenant);
                    _handleConnectMessage(connectMessage);
                    authHandler.handle(Boolean.TRUE);
                } else {
                    authHandler.handle(Boolean.FALSE);
                }
            });
        }
        else {
            String clientID = connectMessage.getClientID();
            String tenant = null;
            if(username == null || username.trim().length()==0) {
                tenant = extractTenant(clientID);
            }
            else {
                tenant = extractTenant(username);
            }
            _initTenant(tenant);
            _handleConnectMessage(connectMessage);
            authHandler.handle(Boolean.TRUE);
        }
    }
    private void _initTenant(String tenant) {
        if(tenant == null)
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

        }
        else {
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
        startKeepAliveTimer(connectMessage.getKeepAlive());
        logger.info("New connection client : " + getClientInfo());
    }

    private void startKeepAliveTimer(int keepAliveSeconds) {
        if(keepAliveSeconds > 0) {
//            stopKeepAliveTimer();
            keepAliveTimeEnded = true;
            /*
             * If the Keep Alive value is non-zero and the Server does not receive a Control Packet from the Client
             * within one and a half times the Keep Alive time period, it MUST disconnect
             */
            long keepAliveMillis = keepAliveSeconds * 1500;
            keepAliveTimerID = vertx.setPeriodic(keepAliveMillis, tid -> {
                if(keepAliveTimeEnded) {
                    logger.debug("keep-alive timer end " + getClientInfo());
                    handleWillMessage();
                    if (keepaliveErrorHandler != null) {
                        keepaliveErrorHandler.handle(clientID);
                    }
                    stopKeepAliveTimer();
                }
                // next time, will close connection
                keepAliveTimeEnded = true;
            });
        }
    }
    private void stopKeepAliveTimer() {
        try {
            logger.debug("keep-alive cancel old timer: " + keepAliveTimerID + " " + getClientInfo());
            boolean removed = vertx.cancelTimer(keepAliveTimerID);
            if (!removed) {
                logger.warn("keep-alive cancel old timer not removed ID: " + keepAliveTimerID + " " + getClientInfo());
            }
        } catch(Throwable e) {
            logger.error("Cannot stop keep-alive timer with ID: "+keepAliveTimerID +" "+ getClientInfo(), e);
        }
    }

    public void resetKeepAliveTimer() {
        keepAliveTimeEnded = false;
    }


    public void handlePublishMessage(PublishMessage publishMessage, Handler<Boolean> completedHandler) {
    	if (authorizationToken != null) {
            AuthorizationClient auth = new AuthorizationClient(vertx.eventBus(), authenticatorAddress);
            auth.authorizePublish(authorizationToken, publishMessage.getTopicName(), permitted -> {
            	if (permitted) {
            		_handlePublishMessage(publishMessage);
            	}
        		if (completedHandler != null) completedHandler.handle(permitted);
            });
    	} else {
    		_handlePublishMessage(publishMessage);
    		if (completedHandler != null) completedHandler.handle(Boolean.TRUE);
    	}
    }
    private void _handlePublishMessage(PublishMessage publishMessage) {
        try {
            // publish always have tenant, if session is not tenantized, tenant is retrieved from topic ([tenant]/to/pi/c)
            String publishTenant = calculatePublishTenant(publishMessage.getTopicName());

            // store retained messages ...
            if(publishMessage.isRetainFlag()) {
                boolean payloadIsEmpty=false;
                ByteBuffer bb = publishMessage.getPayload();
                if(bb!=null) {
                    byte[] bytes = bb.array();
                    if (bytes.length == 0) {
                        payloadIsEmpty = true;
                    }
                }
                if(payloadIsEmpty) {
                    storeManager.deleteRetainMessage(publishTenant, publishMessage.getTopicName());
                } else {
                    storeManager.saveRetainMessage(publishTenant, publishMessage);
                }
            }

            /* It MUST set the RETAIN flag to 0 when a PUBLISH Packet is sent to a Client
             * because it matches an established subscription
             * regardless of how the flag was set in the message it received. */
            publishMessage.setRetainFlag(false);
            Buffer msg = encoder.enc(publishMessage);
            if(tenant == null)
                tenant = "";
            DeliveryOptions opt = new DeliveryOptions().addHeader(TENANT_HEADER, publishTenant);
            vertx.eventBus().publish(ADDRESS, msg, opt);

        } catch(Throwable e) {
            logger.error(e.getMessage());
        }
    }

    private String calculatePublishTenant(String topic) {
        boolean isTenantSession = isTenantSession();
        if(isTenantSession) {
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
            if(idx_end>1 && idx_end > idx_start) {
                t = topic.substring(idx_start, idx_end);
            } else {
//                t = topic.substring(idx_start);
                t = ""; // global tenant...
            }

            return t;
        }
    }

    public void handleSubscribeMessage(SubscribeMessage subscribeMessage, Handler<JsonArray> completedHandler) {
    	if (authorizationToken != null) {
            AuthorizationClient auth = new AuthorizationClient(vertx.eventBus(), authenticatorAddress);
            auth.authorizeSubscribe(authorizationToken, subscribeMessage.subscriptions(), permitted -> {
           		_handleSubscribeMessage(subscribeMessage, permitted);
                if (completedHandler != null) completedHandler.handle(permitted);
            });
    	} else {
    		JsonArray permitted = new JsonArray();
    		for (int i=0; i < subscribeMessage.subscriptions().size(); i++) {
    			permitted.add(true);
    		}
    		_handleSubscribeMessage(subscribeMessage, permitted);
    		if (completedHandler != null) completedHandler.handle(permitted);
    	}
    }
    private void _handleSubscribeMessage(SubscribeMessage subscribeMessage, JsonArray permitted) {
        try {
            final int messageID = subscribeMessage.getMessageID();
            if(this.messageConsumer==null) {
                messageConsumer = vertx.eventBus().consumer(ADDRESS);
                messageConsumer.handler(this);
            }

            // invalidate matching topic cache
            matchingSubscriptionsCache.clear();

            List<SubscribeMessage.Couple> subs = subscribeMessage.subscriptions();
            int indx = 0;
            for(SubscribeMessage.Couple s : subs) {
            	if (permitted.getBoolean(indx++)) {
	                String topicFilter = s.getTopicFilter();
	                Subscription sub = new Subscription();
	                sub.setQos(s.getQos());
	                sub.setTopicFilter(topicFilter);
	                this.subscriptions.put(topicFilter, sub);

                    suscribeMap.put(topicFilter, sub);
	
	                String publishTenant = calculatePublishTenant(topicFilter);
	
	                // check in client wants receive retained message by this topicFilter
	                if(retainSupport) {
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
        } catch(Throwable e) {
            logger.error(e.getMessage());
        }
    }

    private boolean isTenantSession() {
        boolean isTenantSession = tenant!=null && tenant.trim().length()>0;
        return isTenantSession;
    }
    private boolean tenantMatch(Message<Buffer> message) {
        boolean isTenantSession = isTenantSession();
        boolean tenantMatch;
        if(isTenantSession) {
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
            if(tenantMatch) {
                Buffer in = message.body();
                PublishMessage pm = (PublishMessage) decoder.dec(in);
                // filter messages by of subscriptions of this client
                if(pm == null) {
                    logger.warn("PublishMessage is null, message.headers => "+ message.headers().entries()+"");
                }
                else {
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
        if(subs!=null && subs.size()>0) {
            publishMessageToThisClient = true;
            for (Subscription s : subs) {
                int itemQos = s.getQos();
                if (itemQos > maxQos) {
                    maxQos = itemQos;
                }
                // optimization: if qos==2 is alredy **the max** allowed
                if(maxQos == 2)
                    break;
            }
        }

        if(publishMessageToThisClient) {
            // the qos cannot be bigger than the subscribe requested qos ...
            AbstractMessage.QOSType originalQos = publishMessage.getQos();
            int iSentQos = qosUtils.toInt(originalQos);
            int iOkQos = qosUtils.calculatePublishQos(iSentQos, maxQos);
            AbstractMessage.QOSType qos = qosUtils.toQos(iOkQos);
            publishMessage.setQos(qos);
            if(!cleanSession && iSentQos>0) {
                addMessageToQueue(publishMessage);
            }
            sendPublishMessage(publishMessage);
        }
    }

    private List<Subscription> getAllMatchingSubscriptions(String topic) {
        List<Subscription> ret = new ArrayList<>();
//        String topic = pm.getTopicName();
        if(matchingSubscriptionsCache.containsKey(topic)) {
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
        if(publishMessageHandler!=null)
            publishMessageHandler.handle(pm);
    }



    public void handleUnsubscribeMessage(UnsubscribeMessage unsubscribeMessage) {
        try {
            List<String> topicFilterSet = unsubscribeMessage.topicFilters();
            for (String topicFilter : topicFilterSet) {
                if(subscriptions!=null) {
                    subscriptions.remove(topicFilter);
                    matchingSubscriptionsCache.clear();

                    suscribeMap.remove(topicFilter);
                }
            }
        }
        catch(Throwable e) {
            logger.error(e.getMessage());
        }
    }

    /**
     * @Description publish处理
     * @author zhang bo
     * @date 17-12-11
     * @version 1.0
     */
    public void handlerPublishMessage(PublishMessage publishMessage,String clientId,Handler<JsonObject> asyncResultHandler){
        logger.debug("Message payload from " + publishMessage.getPayloadAsString());
        vertx.eventBus().send(publish,new JsonObject(publishMessage.getPayloadAsString())
                .put("topicName",publishMessage.getTopicName()).put("clientId",clientId),(AsyncResult<Message<JsonObject>> rs)->{
                   asyncResultHandler.handle(rs.result().body());
        });

    }

    public void handleDisconnect(DisconnectMessage disconnectMessage) {
        logger.debug("Disconnect from " + clientID +" ...");
        /*
         * TODO: implement this behaviour
         * On receipt of DISCONNECT the Server:
         * - MUST discard any Will Message associated with the current connection without publishing it, as described in Section 3.1.2.5 [MQTT-3.14.4-3].
         * - SHOULD close the Network Connection if the Client has not already done so.
         */
        shutdown();
    }
    public void shutdown() {
        // stop timers
        stopKeepAliveTimer();

        // deallocate this instance ...
        if(messageConsumer!=null && cleanSession) {
            messageConsumer.unregister();
            messageConsumer = null;
        }
//        vertx = null;
    }

    public void handleWillMessage() {
        // publish will message if present ...
        if(willMessage != null) {
            logger.debug("publish will message ... topic[" + willMessage.getTopicName()+"]");
            handlePublishMessage(willMessage, null);
        }
    }

    public String getClientInfo() {
        String clientInfo ="clientID: "+ clientID +", MQTT protocol: "+ protoName +"";
        return clientInfo;
    }

    public String getClientID() {
        return clientID;
    }

    public String getProtoName() {
        return protoName;
    }

    public boolean isCleanSession() {
        return cleanSession;
    }
}
