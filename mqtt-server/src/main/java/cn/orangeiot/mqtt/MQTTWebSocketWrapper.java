package cn.orangeiot.mqtt;

import cn.orangeiot.mqtt.parser.MQTTEncoder;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.WebSocketBase;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage;

/**
 * Created by Giovanni Baleani on 29/06/2015.
 */
public class MQTTWebSocketWrapper extends WebSocketWrapper {

    private static Logger logger = LoggerFactory.getLogger(MQTTWebSocketWrapper.class);

    private MQTTEncoder encoder = new MQTTEncoder();

    public MQTTWebSocketWrapper(WebSocketBase netSocket) {
        super(netSocket);
    }

    public void sendMessageToClient(AbstractMessage message) {
        try {
            Buffer b1 = encoder.enc(message);
            sendMessageToClient(b1);
        } catch(Throwable e) {
            logger.error(e.getMessage(), e);
        }
    }
}
