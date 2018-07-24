package cn.orangeiot.mqtt.parser;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.CorruptedFrameException;
import io.vertx.core.buffer.Buffer;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author andrea
 */
public class MQTTEncoder {
    
    private Map<Byte, DemuxEncoder> m_encoderMap = new HashMap<>();
    
    public MQTTEncoder() {
       m_encoderMap.put(AbstractMessage.CONNECT, new ConnectEncoder());
       m_encoderMap.put(AbstractMessage.CONNACK, new ConnAckEncoder());
       m_encoderMap.put(AbstractMessage.PUBLISH, new PublishEncoder());
       m_encoderMap.put(AbstractMessage.PUBACK, new PubAckEncoder());
       m_encoderMap.put(AbstractMessage.SUBSCRIBE, new SubscribeEncoder());
       m_encoderMap.put(AbstractMessage.SUBACK, new SubAckEncoder());
       m_encoderMap.put(AbstractMessage.UNSUBSCRIBE, new UnsubscribeEncoder());
       m_encoderMap.put(AbstractMessage.DISCONNECT, new DisconnectEncoder());
       m_encoderMap.put(AbstractMessage.PINGREQ, new PingReqEncoder());
       m_encoderMap.put(AbstractMessage.PINGRESP, new PingRespEncoder());
       m_encoderMap.put(AbstractMessage.UNSUBACK, new UnsubAckEncoder());
       m_encoderMap.put(AbstractMessage.PUBCOMP, new PubCompEncoder());
       m_encoderMap.put(AbstractMessage.PUBREC, new PubRecEncoder());
       m_encoderMap.put(AbstractMessage.PUBREL, new PubRelEncoder());
    }

    public Buffer enc(AbstractMessage msg) throws Exception {
        Buffer buff = Buffer.buffer(2+msg.getRemainingLength());
        ByteBuf bb = buff.getByteBuf();
        encode(msg, bb);
        return Buffer.buffer(bb);
    }

    private void encode(AbstractMessage msg, ByteBuf bb) throws Exception {
        DemuxEncoder encoder = m_encoderMap.get(msg.getMessageType());
        if (encoder == null) {
            throw new CorruptedFrameException("Can't find any suitable decoder for message type: " + msg.getMessageType());
        }
        encoder.encode(msg, bb);
    }
}
