package cn.orangeiot.mqtt.parser;

import io.netty.buffer.ByteBuf;
import io.vertx.core.buffer.Buffer;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage;
import org.dna.mqtt.moquette.proto.messages.UnsubscribeMessage;


/**
 *
 * @author andrea
 */
class UnsubscribeEncoder extends DemuxEncoder<UnsubscribeMessage> {

    @Override
    protected void encode(UnsubscribeMessage message, ByteBuf out) {
        if (message.topicFilters().isEmpty()) {
            throw new IllegalArgumentException("Found an unsubscribe message with empty topics");
        }

        if (message.getQos() != AbstractMessage.QOSType.LEAST_ONE) {
            throw new IllegalArgumentException("Expected a message with QOS 1, found " + message.getQos());
        }
        
//        ByteBuf variableHeaderBuff = chc.alloc().buffer(4);
        ByteBuf variableHeaderBuff = Buffer.buffer(4).getByteBuf();
        ByteBuf buff = null;
        try {
            variableHeaderBuff.writeShort(message.getMessageID());
            for (String topic : message.topicFilters()) {
                variableHeaderBuff.writeBytes(Utils.encodeString(topic));
            }

            int variableHeaderSize = variableHeaderBuff.readableBytes();
            byte flags = Utils.encodeFlags(message);
//            buff = chc.alloc().buffer(2 + variableHeaderSize);
            buff = Buffer.buffer(2 + variableHeaderSize).getByteBuf();

            buff.writeByte(AbstractMessage.UNSUBSCRIBE << 4 | flags);
            buff.writeBytes(Utils.encodeRemainingLength(variableHeaderSize));
            buff.writeBytes(variableHeaderBuff);

            out.writeBytes(buff);
        } finally {
            variableHeaderBuff.release();
            buff.release();
        }
    }
    
}
