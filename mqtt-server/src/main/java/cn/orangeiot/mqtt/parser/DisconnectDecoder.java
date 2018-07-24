package cn.orangeiot.mqtt.parser;

import io.netty.buffer.ByteBuf;
import org.dna.mqtt.moquette.proto.messages.DisconnectMessage;

import java.util.List;

/**
 *
 * @author andrea
 */
class DisconnectDecoder extends DemuxDecoder {

    @Override
    void decode(ByteBuf in, List<Object> out) throws Exception {
        //Common decoding part
        in.resetReaderIndex();
        DisconnectMessage message = new DisconnectMessage();
        if (!decodeCommonHeader(message, 0x00, in)) {
            in.resetReaderIndex();
            return;
        }
        out.add(message);
    }
    
}
