package cn.orangeiot.mqtt.parser;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.CorruptedFrameException;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage;
import org.dna.mqtt.moquette.proto.messages.PublishMessage;

import java.util.List;

/**
 *
 * @author andrea
 */
class PublishDecoder extends DemuxDecoder {
    
//    private static Logger LOG = LogManager.getLogger(PublishDecoder.class);

//    @Override
//    void decode(ByteBuf in, List<Object> out) throws Exception {
////        LOG.info("decode invoked with buffer {}", in);
//        in.resetReaderIndex();
//        int startPos = in.readerIndex();
//
//        //Common decoding part
//        PublishMessage message = new PublishMessage();
//        if (!decodeCommonHeader(message, in)) {
////            LOG.info("decode ask for more data after {}", in);
//            in.resetReaderIndex();
//            return;
//        }
//        int remainingLength = message.getRemainingLength();
//
//        //Topic name
//        String topic = Utils.decodeString(in);
//        if (topic == null) {
//            in.resetReaderIndex();
//            return;
//        }
//        message.setTopicName(topic);
//
//        if (message.getQos() == AbstractMessage.QOSType.LEAST_ONE ||
//                message.getQos() == AbstractMessage.QOSType.EXACTLY_ONCE) {
//            message.setMessageID(in.readUnsignedShort());
//        }
//        int stopPos = in.readerIndex();
//
//        //read the payload
//        int payloadSize = remainingLength - (stopPos - startPos - 2) + (Utils.numBytesToEncode(remainingLength) - 1);
//        if (in.readableBytes() < payloadSize) {
//            in.resetReaderIndex();
//            return;
//        }
////        byte[] b = new byte[payloadSize];
//        ByteBuf bb = Unpooled.buffer(payloadSize);
//        in.readBytes(bb);
//        message.setPayload(bb.nioBuffer());
//
//        out.add(message);
//    }

    @Override
    void decode(ByteBuf in, List<Object> out) throws Exception {
//        LOG.info("decode invoked with buffer {}", in);
        in.resetReaderIndex();
        int startPos = in.readerIndex();

        //Common decoding part
        PublishMessage message = new PublishMessage();
        if (!decodeCommonHeader(message, in)) {
//            LOG.info("decode ask for more data after {}", in);
            in.resetReaderIndex();
            return;
        }

        // : reimplement without ctx
//        if (Utils.isMQTT3_1_1(ctx)) {
//            if (message.getQos() == AbstractMessage.QOSType.MOST_ONE && message.isDupFlag()) {
//                //bad protocol, if QoS=0 => DUP = 0
//                throw new CorruptedFrameException("Received a PUBLISH with QoS=0 & DUP = 1, MQTT 3.1.1 violation");
//            }
//
//            if (message.getQos() == AbstractMessage.QOSType.RESERVED) {
//                throw new CorruptedFrameException("Received a PUBLISH with QoS flags setted 10 b11, MQTT 3.1.1 violation");
//            }
//        }

        int remainingLength = message.getRemainingLength();

        //Topic name
        String topic = Utils.decodeString(in);
        if (topic == null) {
            in.resetReaderIndex();
            return;
        }
        if (topic.contains("+") || topic.contains("#")) {
            throw new CorruptedFrameException("Received a PUBLISH with topic containting wild card chars, topic: " + topic);
        }

        message.setTopicName(topic);

        if (message.getQos() == AbstractMessage.QOSType.LEAST_ONE ||
                message.getQos() == AbstractMessage.QOSType.EXACTLY_ONCE) {
            message.setMessageID( in.readUnsignedShort());
        }
        int stopPos = in.readerIndex();

        //read the payload
        int payloadSize = remainingLength - (stopPos - startPos - 2) + (Utils.numBytesToEncode(remainingLength) - 1);
        if (in.readableBytes() < payloadSize) {
            in.resetReaderIndex();
            return;
        }
//        byte[] b = new byte[payloadSize];
        ByteBuf bb = Unpooled.buffer(payloadSize);
        in.readBytes(bb);
        message.setPayload(bb.nioBuffer());

        out.add(message);
    }
}
