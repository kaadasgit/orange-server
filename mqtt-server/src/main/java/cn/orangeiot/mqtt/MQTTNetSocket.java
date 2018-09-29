package cn.orangeiot.mqtt;

import io.netty.channel.ChannelHandlerContext;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.impl.NetSocketInternal;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import io.vertx.core.net.NetSocket;

import java.util.Map;
import java.util.Objects;

/**
 * Created by giovanni on 07/05/2014.
 */
public class MQTTNetSocket extends MQTTSocket {

    private static Logger logger = LogManager.getLogger(MQTTNetSocket.class);

    private NetSocket netSocket;


    public MQTTNetSocket(int timeout, NetSocketInternal soi, Vertx vertx, ConfigParser config, NetSocket netSocket, Map<String, MQTTSession> sessions) {
        super(timeout, soi, vertx, config, sessions, netSocket);
        this.netSocket = netSocket;
    }

    public void start() {
//        netSocket.setWriteQueueMaxSize(1000);
        netSocket.handler(this);
        netSocket.exceptionHandler(event -> {
            String clientInfo = getClientInfo();
            logger.error(clientInfo + ", net-socket closed ... " + netSocket.writeHandlerID() + " error: " + event.getMessage(), event.getCause());
            handleWillMessage();
            if (Objects.nonNull(session) && Objects.nonNull(session.getClientID()))
                checkDevice(session.getClientID(), "offline");//离线狀態
            shutdown();
        });
        netSocket.closeHandler(aVoid -> {
            String clientInfo = getClientInfo();
            logger.debug(clientInfo + ", net-socket closed ... " + netSocket.writeHandlerID());
            handleWillMessage();
            if (Objects.nonNull(session) && Objects.nonNull(session.getClientID()))
                checkDevice(session.getClientID(), "offline");//离线狀態
            shutdown();
        });
    }


    @SuppressWarnings("Duplicates")
    @Override
    protected void sendMessageToClient(Buffer bytes) {
        try {
            if (netSocket != null) {
                netSocket.write(bytes);
                if (netSocket.writeQueueFull()) {
                    netSocket.pause();
                    netSocket.drainHandler(done -> netSocket.resume());
                }
            }
        } catch (Throwable e) {
            logger.error(e.getMessage());
        }
    }

    protected void closeConnection() {
        logger.debug("net-socket will be closed ... " + netSocket.writeHandlerID());
        netSocket.close();
    }

}
