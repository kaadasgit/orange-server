package cn.orangeiot.mqtt.bridge;

import cn.orangeiot.mqtt.MQTTSession;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.WebSocket;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import io.vertx.core.net.*;

/**
 * Created by Giovanni Bleani on 15/07/2015.
 */
public class EventBusBridgeWebsocketClientVerticle extends AbstractVerticle implements Handler<WebSocket> {

    private static Logger logger = LogManager.getLogger(EventBusBridgeWebsocketClientVerticle.class);

    private String address;
    private HttpClient netClient;
    private String remoteBridgeHost;
    private Integer remoteBridgePort;
    private String remoteBridgePath;
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
        remoteBridgeHost = conf.getString("remote_bridge_host", "iot.eimware.it");
        remoteBridgePort = conf.getInteger("remote_bridge_port", 7007);
        remoteBridgePath = conf.getString("remote_bridge_path", "/");
        tenant = conf.getString("remote_bridge_tenant");
        idleTimeout = conf.getInteger("socket_idle_timeout", 120);
        ssl_cert_key = conf.getString("ssl_cert_key");
        ssl_cert = conf.getString("ssl_cert");
        ssl_trust = conf.getString("ssl_trust");


        createClient();
        connect();
        netClient.websocket(remoteBridgePort, remoteBridgeHost, remoteBridgePath, this);
        connectionTimerID = vertx.setPeriodic(connectTimeout*2, aLong -> {
            checkConnection();
        });
    }
    private void createClient() {
        // [WebSocket <- BUS] listen BUS write to WebSocket
        HttpClientOptions opt = new HttpClientOptions()
                .setConnectTimeout(connectTimeout) // (The default value of connect timeout = 60000 ms) we set to 1 second
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
//            tenant = new CertInfo(ssl_cert).getTenant();
        }

        netClient = vertx.createHttpClient(opt);
    }

    private void connect() {
        if(!connecting) {
            connecting = true;
            netClient.websocket(remoteBridgePort, remoteBridgeHost, remoteBridgePath, this, e -> {
                connecting = false;
                connected = false;
                String msg = "Bridge Client - not connected to server [" + remoteBridgeHost + ":" + remoteBridgePort +""+ remoteBridgePath +"]";
                if (e != null) {
                    logger.error(msg, e);
                } else {
                    logger.error(msg);
                }
            });
        }
    }

    private void checkConnection() {
        if(!connected) {
            logger.info("Bridge Client - try to reconnect to server [" + remoteBridgeHost + ":" + remoteBridgePort +""+ remoteBridgePath +"] ... " + connectionTimerID);
            connect();
        }
    }

    @Override
    public void handle(WebSocket webSocket) {
        connecting = false;
        connected = true;
        logger.info("Bridge Client - connected to server [" + remoteBridgeHost + ":" + remoteBridgePort + "]");

        webSocket.write(Buffer.buffer( tenant + "\n" ));
        webSocket.write(Buffer.buffer( "START SESSION" + "\n" ));
        webSocket.pause();

        final EventBusWebsocketBridge ebnb = new EventBusWebsocketBridge(webSocket, vertx.eventBus(), address);
        webSocket.closeHandler(aVoid -> {
            logger.error("Bridge Client - closed connection from server [" + remoteBridgeHost + ":" + remoteBridgePort + "]" + webSocket.textHandlerID());
            ebnb.stop();
            connected = false;
        });
        webSocket.exceptionHandler(throwable -> {
            logger.error("Bridge Client - Exception: " + throwable.getMessage(), throwable);
            ebnb.stop();
            connected = false;
        });
        ebnb.setTenant(tenant);
        ebnb.start();

        logger.info("Bridge Client - bridgeUUID: "+ ebnb.getBridgeUUID());
        webSocket.resume();
    }

    @Override
    public void stop() throws Exception {
        vertx.cancelTimer(connectionTimerID);
        connected = false;
    }

}
