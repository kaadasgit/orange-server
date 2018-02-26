package cn.orangeiot.mqtt.bridge;

import cn.orangeiot.mqtt.MQTTSession;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.ClientAuth;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetServerOptions;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.core.parsetools.RecordParser;

/**
 * Created by Giovanni Baleani on 15/07/2015.
 */
public class EventBusBridgeServerVerticle extends AbstractVerticle {

    private static Logger logger = LogManager.getLogger(EventBusBridgeServerVerticle.class);

    private String address;
    private NetServer netServer;
    private int localBridgePort;
    private int idleTimeout;
    private String ssl_cert_key;
    private String ssl_cert;
    private String ssl_trust;

    @Override
    public void start() throws Exception {
        address = MQTTSession.ADDRESS;

        JsonObject conf = config();

        localBridgePort = conf.getInteger("local_bridge_port", 7007);
        idleTimeout = conf.getInteger("socket_idle_timeout", 120);
        ssl_cert_key = conf.getString("ssl_cert_key");
        ssl_cert = conf.getString("ssl_cert");
        ssl_trust = conf.getString("ssl_trust");


        // [TCP -> BUS] listen TCP publish to BUS
        NetServerOptions opt = new NetServerOptions()
                .setTcpKeepAlive(true)
                .setIdleTimeout(idleTimeout)
                .setPort(localBridgePort)
        ;

        if(ssl_cert_key != null && ssl_cert != null && ssl_trust != null) {
            opt.setSsl(true).setClientAuth(ClientAuth.REQUIRED)
                .setPemKeyCertOptions(new PemKeyCertOptions()
                    .setKeyPath(ssl_cert_key)
                    .setCertPath(ssl_cert)
                )
                .setPemTrustOptions(new PemTrustOptions()
                    .addCertPath(ssl_trust)
                )
            ;
        }

        netServer = vertx.createNetServer(opt);
        netServer.connectHandler(sock -> {
            final EventBusNetBridge ebnb = new EventBusNetBridge(sock, vertx.eventBus(), address);
            sock.closeHandler(aVoid -> {
                logger.info("Bridge Server - closed connection from client ip: " + sock.remoteAddress());
                ebnb.stop();
            });
            sock.exceptionHandler(throwable -> {
                logger.error("Bridge Server - Exception: " + throwable.getMessage(), throwable);
                ebnb.stop();
            });

            logger.info("Bridge Server - new connection from client ip: " + sock.remoteAddress());

            RecordParser parser = ebnb.initialHandhakeProtocolParser();
            sock.handler(parser::handle);

        }).listen();
    }

    @Override
    public void stop() throws Exception {
        netServer.close();
    }

}
