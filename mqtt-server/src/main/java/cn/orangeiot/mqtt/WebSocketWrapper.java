package cn.orangeiot.mqtt;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.WebSocketBase;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Created by giova_000 on 29/06/2015.
 */
public class WebSocketWrapper {

    private static Logger logger = LoggerFactory.getLogger(WebSocketWrapper.class);

    private WebSocketBase webSocket;

    public WebSocketWrapper(WebSocketBase netSocket) {
        if(netSocket==null)
            throw new IllegalArgumentException("MQTTWebSocketWrapper: webSocket cannot be null");
        this.webSocket = netSocket;
    }

    public void sendMessageToClient(Buffer bytes) {
        try {
            webSocket.write(bytes);
            if (webSocket.writeQueueFull()) {
                webSocket.pause();
                webSocket.drainHandler(done -> webSocket.resume() );
            }
        } catch(Throwable e) {
            logger.error(e.getMessage(), e);
        }
    }

    public void stop() {
        // stop writing to socket
//        webSocket.drainHandler(null);
    }
}
