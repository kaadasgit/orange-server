package cn.orangeiot.mqtt.bridge;

import cn.orangeiot.mqtt.MQTTSession;
import cn.orangeiot.mqtt.security.CertInfo;
import cn.orangeiot.mqtt.MQTTNetSocketWrapper;
import cn.orangeiot.mqtt.NetSocketWrapper;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.*;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import io.vertx.core.net.NetSocket;
import io.vertx.core.parsetools.RecordParser;
import io.vertx.core.streams.Pump;

import java.util.UUID;

/**
 * Created by Giovanni Baleani on 15/07/2015.
 */
public class EventBusNetBridge {

    private static Logger logger = LogManager.getLogger(EventBusNetBridge.class);

    private static final String BR_HEADER = "bridged";

    private NetSocket netSocket;
    private EventBus eventBus;
    private String eventBusAddress;
    private String tenant;
    private DeliveryOptions deliveryOpt;
    private MessageConsumer<Buffer> consumer;
    private MessageProducer<Buffer> producer;
    private Pump fromRemoteTcpToLocalBus;
    private NetSocketWrapper netSocketWrapper;
    private String bridgeUUID;

    public EventBusNetBridge(NetSocket netSocket, EventBus eventBus, String eventBusAddress) {
        this.eventBus = eventBus;
        this.netSocket = netSocket;
        this.eventBusAddress = eventBusAddress;
        this.bridgeUUID = UUID.randomUUID().toString();
    }

    public void init() {
        deliveryOpt = new DeliveryOptions().addHeader(BR_HEADER, bridgeUUID);
        if(tenant!=null) {
            deliveryOpt.addHeader(MQTTSession.TENANT_HEADER, tenant);
        }
        consumer = eventBus.consumer(eventBusAddress);
        producer = eventBus.publisher(eventBusAddress, deliveryOpt);
        fromRemoteTcpToLocalBus = new MqttPump(netSocket, producer);
        netSocketWrapper = new MQTTNetSocketWrapper(netSocket);
    }

    public void start() {
        init();
        netSocket.pause();
        consumer.pause();
        // from remote tcp to local bus
        fromRemoteTcpToLocalBus.start();

        // from local bus to remote tcp
        consumer.handler(bufferMessage -> {
            boolean isBridged = isBridged(bufferMessage);
            if (!isBridged) {
                boolean tenantMatch = tenantMatch(bufferMessage);
                if(tenantMatch) {
                    netSocketWrapper.sendMessageToClient(bufferMessage.body());
                }
            }
        });
        consumer.resume();
        netSocket.resume();
    }

    private boolean isBridged(Message<Buffer> bufferMessage) {
        // try to comment uuid check... this is more restrictive policy
        // but we hope can prevent loop in very rare cases...
        boolean isBridged = bufferMessage.headers() != null
                && bufferMessage.headers().contains(BR_HEADER)
                && bufferMessage.headers().get(BR_HEADER).equals(bridgeUUID)
                ;
        return isBridged;
    }

    // TODO: this method is equal to MQTTSession.isTenantSession, need refactoring
    private boolean isTenantSession() {
        boolean isTenantSession = tenant!=null && tenant.trim().length()>0;
        return isTenantSession;
    }
    // TODO: this method is equal to MQTTSession.tenantMatch, need refactoring
    private boolean tenantMatch(Message<Buffer> message) {
        boolean isTenantSession = isTenantSession();
        boolean tenantMatch;
        if(isTenantSession) {
            boolean containsTenantHeader = message.headers().contains(MQTTSession.TENANT_HEADER);
            if (containsTenantHeader) {
                String tenantHeaderValue = message.headers().get(MQTTSession.TENANT_HEADER);
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

    String getBridgeUUID() {
        return bridgeUUID;
    }

    public void stop() {
        // from remote tcp to local bus
        fromRemoteTcpToLocalBus.stop();
        // from local bus to remote tcp
        netSocketWrapper.stop();// stop write to remote tcp socket
        consumer.handler(null);// stop read from bus
    }



    public void setTenant(String tenant) {
        this.tenant = tenant;
    }

    public String getTenant() {
        return tenant;
    }


    public RecordParser initialHandhakeProtocolParser() {
        NetSocket sock = netSocket;
        final RecordParser parser = RecordParser.newDelimited("\n", h -> {
            String cmd = h.toString();
            if("START SESSION".equalsIgnoreCase(cmd)) {
                sock.pause();
                start();
                logger.info("Bridge Server - start session with " +
                        "tenant: " + getTenant() +
                        ", ip: " + sock.remoteAddress() +
                        ", bridgeUUID: " + getBridgeUUID()
                );
                sock.resume();
            } else {
                String tenant = cmd;
                String tenantFromCert = new CertInfo(sock).getTenant();
                if(tenantFromCert != null)
                    tenant = tenantFromCert;

                setTenant(tenant);
            }
        });
        return parser;
    }
}
