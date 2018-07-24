package cn.orangeiot.mqtt;

import io.vertx.core.buffer.Buffer;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import io.vertx.core.net.NetSocket;

/**
 * Created by giova_000 on 29/06/2015.
 */
public class NetSocketWrapper {

    private static Logger logger = LogManager.getLogger(NetSocketWrapper.class);

    private NetSocket netSocket;

    public NetSocketWrapper(NetSocket netSocket) {
        if(netSocket==null)
            throw new IllegalArgumentException("MQTTNetSocketWrapper: netSocket cannot be null");
        this.netSocket = netSocket;
    }

    // : this method is equals to MQTTNetSocket.sendMessageToClient... need refactoring
    public void sendMessageToClient(Buffer bytes) {
        try {
            netSocket.write(bytes);
            if (netSocket.writeQueueFull()) {
                netSocket.pause();
                netSocket.drainHandler( done -> netSocket.resume() );
            }
        } catch(Throwable e) {
            logger.error(e.getMessage(), e);
        }
    }

    public void stop() {
        // stop writing to socket
        netSocket.drainHandler(null);
    }
}
