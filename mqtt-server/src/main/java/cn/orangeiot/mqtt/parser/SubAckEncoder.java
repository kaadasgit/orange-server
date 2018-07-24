package cn.orangeiot.mqtt.parser;

import io.netty.buffer.ByteBuf;
import io.vertx.core.buffer.Buffer;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage;
import org.dna.mqtt.moquette.proto.messages.SubAckMessage;

/**
 *
 * @author andrea
 */
class SubAckEncoder extends DemuxEncoder<SubAckMessage> {

    @Override
    protected void encode(SubAckMessage message, ByteBuf out) {
        if (message.types().isEmpty()) {
            throw new IllegalArgumentException("Found a suback message with empty topics");
        }

        int variableHeaderSize = 2 + message.types().size();
//        ByteBuf buff = chc.alloc().buffer(6 + variableHeaderSize);
        ByteBuf buff = Buffer.buffer(6 + variableHeaderSize).getByteBuf();
        try {
            buff.writeByte(AbstractMessage.SUBACK << 4 );
            buff.writeBytes(Utils.encodeRemainingLength(variableHeaderSize));
            buff.writeShort(message.getMessageID());
            for (AbstractMessage.QOSType c : message.types()) {
            	if (c == AbstractMessage.QOSType.FAILURE) {
            		buff.writeByte(AbstractMessage.QOSType.FAILURE_VALUE);
            	} else {
            		buff.writeByte(c.ordinal());
            	}
            }

            out.writeBytes(buff);
        } finally {
            buff.release();
        }
    }
    
}
