package cn.orangeiot.mqtt;

import cn.orangeiot.mqtt.bridge.EventBusBridgeWebsocketClientVerticle;
import cn.orangeiot.mqtt.bridge.EventBusBridgeClientVerticle;
import cn.orangeiot.mqtt.bridge.EventBusBridgeServerVerticle;
import cn.orangeiot.mqtt.bridge.EventBusBridgeWebsocketServerVerticle;
import cn.orangeiot.mqtt.persistence.StoreVerticle;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetServerOptions;
import io.vertx.core.net.PemKeyCertOptions;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by giovanni on 11/04/2014.
 * The Main Verticle
 */
public class MQTTBroker extends AbstractVerticle {

    private Logger logger = LogManager.getLogger(MQTTBroker.class);

    private void deployVerticle(String c, DeploymentOptions opt) {
        vertx.deployVerticle(c, opt,
                result -> {
                    if (result.failed()) {
                        result.cause().printStackTrace();
                    } else {
                        String deploymentID = result.result();
                        logger.debug(c + ": " + deploymentID);
                    }
                }
        );
    }

    private void deployVerticle(Class c, DeploymentOptions opt) {
        vertx.deployVerticle(c.getName(), opt,
                result -> {
                    if (result.failed()) {
                        result.cause().printStackTrace();
                    } else {
                        String deploymentID = result.result();
                        logger.debug(c.getSimpleName() + ": " + deploymentID);
                    }
                }
        );
    }

    private void deployAuthorizationVerticle(JsonObject config, int instances) {
//        String clazz = config.getString("verticle", OAuth2AuthenticatorVerticle.class.getName());
        String clazz = config.getString("verticle");
        deployVerticle(clazz,
                new DeploymentOptions()
                        .setWorker(true)
                        .setInstances(instances)
                        .setConfig(config)
        );
    }

    private void deployStoreVerticle(int instances) {
        deployVerticle(StoreVerticle.class,
                new DeploymentOptions().setWorker(false).setInstances(instances)
        );
    }

    private void deployBridgeServerVerticle(JsonObject config, int instances) {
        Boolean useWebsocket = config.getBoolean("websocket_enabled", false);
        Class c;
        if (useWebsocket) {
            c = EventBusBridgeWebsocketServerVerticle.class;
        } else {
            c = EventBusBridgeServerVerticle.class;
        }
        deployVerticle(c, new DeploymentOptions().setWorker(false).setInstances(instances).setConfig(config));
    }

    private void deployBridgeClientVerticle(JsonObject config, int instances) {
        Boolean useWebsocket = config.getBoolean("websocket_enabled", false);
        Class c;
        if (useWebsocket) {
            c = EventBusBridgeWebsocketClientVerticle.class;
        } else {
            c = EventBusBridgeClientVerticle.class;
        }
        deployVerticle(c, new DeploymentOptions().setWorker(false).setInstances(instances).setConfig(config));
    }

    @Override
    public void stop() {
    }


    @Override
    public void start() {
        try {
            JsonObject config = config();

            // 1 store x 1 broker
            deployStoreVerticle(1);

            // 2 bridge server
            if (config.containsKey("bridge_server")) {
                JsonObject bridgeServerConf = config.getJsonObject("bridge_server", new JsonObject());
                deployBridgeServerVerticle(bridgeServerConf, 1);
            }

            // 3 bridge client
            if (config.containsKey("bridge_client")) {
                JsonObject bridgeClientConf = config.getJsonObject("bridge_client", new JsonObject());
                deployBridgeClientVerticle(bridgeClientConf, 1);
            }

            // 4 authenticators
//            if(config.containsKey("authenticators")) {
//                JsonArray authenticators = config.getJsonArray("authenticators", new JsonArray());
//                int size = authenticators.size();
//                for(int i=0; i<size; i++) {
//                    JsonObject authConf = authenticators.getJsonObject(i);
//                    deployAuthorizationVerticle(authConf, 1);
//                }
//            }


            JsonArray brokers = config.getJsonArray("brokers");
            for (int i = 0; i < brokers.size(); i++) {
                JsonObject brokerConf = brokers.getJsonObject(i);
                brokerConf.put("send_publish_message", config.getString("send_publish_message"));
                ConfigParser c = new ConfigParser(brokerConf);
                boolean wsEnabled = c.isWsEnabled();
                if (wsEnabled) {
                    // MQTT over WebSocket
                    startWebsocketServer(c);
                } else {
                    // MQTT over TCP
                    startTcpServer(c);
                }
                logger.info(
                        "Startd Broker ==> [port: " + c.getPort() + "]" +
                                " [" + c.getFeatursInfo() + "] " +
                                " [socket_idle_timeout:" + c.getSocketIdleTimeout() + "] "
                );
            }

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

    }


    private void startTcpServer(ConfigParser c) {
        int port = c.getPort();
        String keyPath = c.getTlsKeyPath();
        String certPath = c.getTlsCertPath();
        boolean tlsEnabled = c.isTlsEnabled();
        int idleTimeout = c.getSocketIdleTimeout();

        // MQTT over TCP
        NetServerOptions opt = new NetServerOptions()
                .setTcpKeepAlive(true)
                .setIdleTimeout(idleTimeout) // in seconds; 0 means "don't timeout".
                .setPort(port);

        if (tlsEnabled) {
            opt.setSsl(true).setPemKeyCertOptions(new PemKeyCertOptions()
                    .setKeyPath(keyPath)
                    .setCertPath(certPath)
            );
        }
        NetServer netServer = vertx.createNetServer(opt);
        Map<String, MQTTSession> sessions = new ConcurrentHashMap<>();
        netServer.connectHandler(netSocket -> {
            MQTTNetSocket mqttNetSocket = new MQTTNetSocket(vertx, c, netSocket, sessions);
            mqttNetSocket.start();
        }).listen();

    }

    private void startWebsocketServer(ConfigParser c) {
        int port = c.getPort();
        String wsSubProtocols = c.getWsSubProtocols();
        String keyPath = c.getTlsKeyPath();
        String certPath = c.getTlsCertPath();
        boolean tlsEnabled = c.isTlsEnabled();
        int idleTimeout = c.getSocketIdleTimeout();

        HttpServerOptions httpOpt = new HttpServerOptions()
                .setTcpKeepAlive(true)
                .setIdleTimeout(idleTimeout) // in seconds; 0 means "don't timeout".
                .setWebsocketSubProtocols(wsSubProtocols)
                .setPort(port);
        if (tlsEnabled) {
            httpOpt.setSsl(true);
            httpOpt.setPemKeyCertOptions(new PemKeyCertOptions()
                    .setKeyPath(keyPath)
                    .setCertPath(certPath)
            );
        }
        HttpServer http = vertx.createHttpServer(httpOpt);
        Map<String, MQTTSession> sessions = new HashMap<>();
        http.websocketHandler(serverWebSocket -> {
            MQTTWebSocket mqttWebSocket = new MQTTWebSocket(vertx, c, serverWebSocket, sessions);
            mqttWebSocket.start();
        }).listen();
    }
}
