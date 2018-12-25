package cn.orangeiot.mqtt.parser;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.CorruptedFrameException;
import io.vertx.core.buffer.Buffer;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author andrea
 */
public class MQTTDecoder {

    //3 = 3.1, 4 = 3.1.1
//    static final AttributeKey<Integer> PROTOCOL_VERSION = new AttributeKey<Integer>("version");

    private Map<Byte, DemuxDecoder> m_decoderMap = new HashMap<>();

    public MQTTDecoder() {
       m_decoderMap.put(AbstractMessage.CONNECT, new ConnectDecoder());
       m_decoderMap.put(AbstractMessage.CONNACK, new ConnAckDecoder());
       m_decoderMap.put(AbstractMessage.PUBLISH, new PublishDecoder());
       m_decoderMap.put(AbstractMessage.PUBACK, new PubAckDecoder());
       m_decoderMap.put(AbstractMessage.SUBSCRIBE, new SubscribeDecoder());
       m_decoderMap.put(AbstractMessage.SUBACK, new SubAckDecoder());
       m_decoderMap.put(AbstractMessage.UNSUBSCRIBE, new UnsubscribeDecoder());
       m_decoderMap.put(AbstractMessage.DISCONNECT, new DisconnectDecoder());
       m_decoderMap.put(AbstractMessage.PINGREQ, new PingReqDecoder());
       m_decoderMap.put(AbstractMessage.PINGRESP, new PingRespDecoder());
       m_decoderMap.put(AbstractMessage.UNSUBACK, new UnsubAckDecoder());
       m_decoderMap.put(AbstractMessage.PUBCOMP, new PubCompDecoder());
       m_decoderMap.put(AbstractMessage.PUBREC, new PubRecDecoder());
       m_decoderMap.put(AbstractMessage.PUBREL, new PubRelDecoder());
    }

    public AbstractMessage dec(Buffer in) throws Exception {
        ArrayList<Object> out = new ArrayList<>();
        decode(in.getByteBuf(), out);
        if(out.size()==1) {
            Object o = out.get(0);
            if (o instanceof AbstractMessage) {
                return (AbstractMessage) o;
            }
        } else if(out.size()>1) {
            throw new CorruptedFrameException("Parsed too many messages.");
        }
        return null;
    }
    private void decode(ByteBuf in, List<Object> out) throws Exception {
        in.markReaderIndex();
        if (!Utils.checkHeaderAvailability(in)) {
            in.resetReaderIndex();
            return;
        }
        in.resetReaderIndex();

        byte messageType = Utils.readMessageType(in);

        DemuxDecoder decoder = m_decoderMap.get(messageType);
        if (decoder == null) {
            throw new CorruptedFrameException("Can't find any suitable decoder for message type: " + messageType);
        }
        decoder.decode(in, out);
    }

//    private byte readMessageType(ByteBuf in) {
//        byte h1 = in.readByte();
//        byte messageType = (byte) ((h1 & 0x00F0) >> 4);
//        return messageType;
//    }
//    private boolean checkHeaderAvailability(ByteBuf in) {
//        if (in.readableBytes() < 1) {
//            return false;
//        }
//        //byte h1 = in.get();
//        //byte messageType = (byte) ((h1 & 0x00F0) >> 4);
//        in.skipBytes(1); //skip the messageType byte
//
//        int remainingLength = Utils.decodeRemainingLenght(in);
//        if (remainingLength == -1) {
//            return false;
//        }
//
//        //check remaining length
//        if (in.readableBytes() < remainingLength) {
//            return false;
//        }
//
//        //return messageType == type ? MessageDecoderResult.OK : MessageDecoderResult.NOT_OK;
//        return true;
//    }
}
