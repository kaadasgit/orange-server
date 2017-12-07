package cn.orangeiot.mqtt.bridge;

import cn.orangeiot.mqtt.MQTTSession;
import cn.orangeiot.mqtt.security.CertInfo;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.net.*;

/**
 * Created by Giovanni Bleani on 15/07/2015.
 */
public class EventBusBridgeClientVerticle extends AbstractVerticle implements Handler<AsyncResult<NetSocket>> {

    private static Logger logger = LoggerFactory.getLogger(EventBusBridgeClientVerticle.class);
    
    private String address;
    private NetClient netClient;
    private String remoteBridgeHost;
    private Integer remoteBridgePort;
    private long connectionTimerID;
    private boolean connected;
    private boolean connecting;
    private String tenant;
    private int idleTimeout;
    private String ssl_cert_key;
    private String ssl_cert;
    private String ssl_trust;
    private int connectTimeout = 1000;

    @Override
    public void start() throws Exception {
        address = MQTTSession.ADDRESS;

        JsonObject conf = config();
        remoteBridgeHost = conf.getString("remote_bridge_host", "localhost");
        remoteBridgePort = conf.getInteger("remote_bridge_port", 7007);
        tenant = conf.getString("remote_bridge_tenant");
        idleTimeout = conf.getInteger("socket_idle_timeout", 120);
        ssl_cert_key = conf.getString("ssl_cert_key");
        ssl_cert = conf.getString("ssl_cert");
        ssl_trust = conf.getString("ssl_trust");

        createClient();
        connect();
        connectionTimerID = vertx.setPeriodic(connectTimeout*2, aLong -> {
            checkConnection();
        });
    }
    private void createClient() {
        // [TCP <- BUS] listen BUS write to TCP
        NetClientOptions opt = new NetClientOptions()
                .setConnectTimeout(connectTimeout) // 60 seconds
                .setTcpKeepAlive(true)
                .setIdleTimeout(idleTimeout)
                ;

        if(ssl_cert_key != null && ssl_cert != null && ssl_trust != null) {
            opt.setSsl(true)
                    .setPemKeyCertOptions(new PemKeyCertOptions()
                            .setKeyPath(ssl_cert_key)
                            .setCertPath(ssl_cert)
                    )
                    .setPemTrustOptions(new PemTrustOptions()
                            .addCertPath(ssl_trust)
                    )
            ;
            tenant = new CertInfo(ssl_cert).getTenant();
        }

        netClient = vertx.createNetClient(opt);
    }
    private void connect() {
        if(!connecting) {
            connecting = true;
            netClient.connect(remoteBridgePort, remoteBridgeHost, this);
        }
    }

    private void checkConnection() {
        if(!connected) {
            logger.info("Bridge Client - try to reconnect to server [" + remoteBridgeHost + ":" + remoteBridgePort + "] ... " + connectionTimerID);
            connect();
        }
    }

    @Override
    public void handle(AsyncResult<NetSocket> netSocketAsyncResult) {
        connecting = false;
        if (netSocketAsyncResult.succeeded()) {
            NetSocket netSocket = netSocketAsyncResult.result();
            connected = true;
            logger.info("Bridge Client - connected to server [" + remoteBridgeHost + ":" + remoteBridgePort + "] " + netSocket.writeHandlerID());

            netSocket.write(tenant + "\n");
            netSocket.write("START SESSION" + "\n");
            netSocket.pause();

            final EventBusNetBridge ebnb = new EventBusNetBridge(netSocket, vertx.eventBus(), address);
            netSocket.closeHandler(aVoid -> {
                logger.info("Bridge Client - closed connection from server [" + remoteBridgeHost + ":" + remoteBridgePort + "] " + netSocket.writeHandlerID());
                ebnb.stop();
                connected = false;
            });
            netSocket.exceptionHandler(throwable -> {
                logger.error("Bridge Client - Exception: " + throwable.getMessage(), throwable);
                ebnb.stop();
                connected = false;
            });
            ebnb.setTenant(tenant);
            ebnb.start();

            logger.info("Bridge Client - bridgeUUID: "+ ebnb.getBridgeUUID());
            netSocket.resume();
        } else {
            connected = false;
            String msg = "Bridge Client - not connected to server [" + remoteBridgeHost + ":" + remoteBridgePort +"]";
            Throwable e = netSocketAsyncResult.cause();
            if (e != null) {
                logger.error(msg, e);
            } else {
                logger.error(msg);
            }
        }
    }


    @Override
    public void stop() throws Exception {
        vertx.cancelTimer(connectionTimerID);
        connected = false;
    }

}
