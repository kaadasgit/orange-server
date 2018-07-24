package cn.orangeiot.mqtt.parser;

import org.dna.mqtt.moquette.proto.messages.MessageIDMessage;
import org.dna.mqtt.moquette.proto.messages.PubAckMessage;

/**
 *
 * @author andrea
 */
class PubAckDecoder extends MessageIDDecoder {

    @Override
    protected MessageIDMessage createMessage() {
        return new PubAckMessage();
    }
    
}
