package cn.orangeiot.mqtt.parser;

import io.netty.buffer.ByteBuf;
import org.dna.mqtt.moquette.proto.messages.MessageIDMessage;

import java.util.List;

/**
 *
 * @author andrea
 */
abstract class MessageIDDecoder extends DemuxDecoder {
    
    protected abstract MessageIDMessage createMessage();

    @Override
    void decode(ByteBuf in, List<Object> out) throws Exception {
        in.resetReaderIndex();
        //Common decoding part
        MessageIDMessage message = createMessage();
        if (!decodeCommonHeader(message, 0x00, in)) {
            in.resetReaderIndex();
            return;
        }
        
        //read  messageIDs
        message.setMessageID(in.readUnsignedShort());
        out.add(message);
    }
    
}
