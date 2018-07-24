package cn.orangeiot.mqtt.parser;

import org.dna.mqtt.moquette.proto.messages.MessageIDMessage;
import org.dna.mqtt.moquette.proto.messages.PubRecMessage;

/**
 *
 * @author andrea
 */
class PubRecDecoder extends MessageIDDecoder {

    @Override
    protected MessageIDMessage createMessage() {
        return new PubRecMessage();
    }
}
