package cn.orangeiot.mqtt;

import cn.orangeiot.mqtt.parser.MQTTEncoder;
import io.vertx.core.buffer.Buffer;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import io.vertx.core.net.NetSocket;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage;

/**
 * Created by giova_000 on 29/06/2015.
 */
public class MQTTNetSocketWrapper extends NetSocketWrapper {

    private static Logger logger = LogManager.getLogger(MQTTNetSocketWrapper.class);

    private MQTTEncoder encoder = new MQTTEncoder();

    public MQTTNetSocketWrapper(NetSocket netSocket) {
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
