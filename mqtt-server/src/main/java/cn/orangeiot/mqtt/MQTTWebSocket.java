package cn.orangeiot.mqtt;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.impl.NetSocketInternal;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.util.Map;

/**
 * Created by giovanni on 07/05/2014.
 */
public class MQTTWebSocket extends MQTTSocket {

    private static Logger logger = LogManager.getLogger(MQTTWebSocket.class);

    private ServerWebSocket netSocket;

    public MQTTWebSocket(int timeout, NetSocketInternal soi, Vertx vertx, ConfigParser config, ServerWebSocket netSocket, Map<String, MQTTSession> sessions) {
        super(timeout, soi, vertx, config, sessions);
        this.netSocket = netSocket;
    }

    public void start() {
        netSocket.handler(this);
        netSocket.exceptionHandler(event -> {
            String clientInfo = getClientInfo();
            logger.info(clientInfo + ", web-socket closed ... " + netSocket.binaryHandlerID() + " error: " + event.getMessage());
            handleWillMessage();
            shutdown();
        });
        netSocket.closeHandler(aVoid -> {
            String clientInfo = getClientInfo();
            logger.info(clientInfo + ", web-socket closed ... " + netSocket.binaryHandlerID() + " " + netSocket.textHandlerID());
            shutdown();
        });
    }

    @Override
    protected void sendMessageToClient(Buffer bytes) {
        try {
            netSocket.write(bytes);
            if (netSocket.writeQueueFull()) {
                netSocket.pause();
                netSocket.drainHandler(done -> netSocket.resume());
            }
        } catch (Throwable e) {
            logger.error(e.getMessage());
        }
    }

    protected void closeConnection() {
        logger.debug("web-socket will be closed ... " + netSocket.binaryHandlerID() + " " + netSocket.textHandlerID());
        if (session != null) {
            session.handleWillMessage();
        }
        netSocket.close();
    }
}
