package cn.orangeiot.mqtt;

import io.vertx.core.json.JsonObject;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage;
import org.dna.mqtt.moquette.proto.messages.PublishMessage;

import java.nio.ByteBuffer;

/**
 * Created by giovanni on 14/04/14.
 * JSON Utility class
 */
public class MQTTJson {

    public boolean isDeserializable(JsonObject json) {
        boolean ret = (
               json.containsKey("topicName")
            && json.containsKey("payload")
        );
        return ret;
    }

    public JsonObject serializePublishMessage(PublishMessage publishMessage) {
        JsonObject ret = new JsonObject();

        ret.put("topicName", publishMessage.getTopicName());
        ret.put("qos", publishMessage.getQos().name());
        ret.put("payload", publishMessage.getPayload().array());
        if(publishMessage.getQos() == AbstractMessage.QOSType.LEAST_ONE || publishMessage.getQos() == AbstractMessage.QOSType.EXACTLY_ONCE) {
            ret.put("messageID", publishMessage.getMessageID());
        }
        return ret;
    }
    public PublishMessage deserializePublishMessage(JsonObject json) {
        PublishMessage ret = new PublishMessage();
        ret.setTopicName(json.getString("topicName"));
        AbstractMessage.QOSType qos = AbstractMessage.QOSType.valueOf(json.getString("qos"));
        ret.setQos(qos);
        byte[] payload = json.getBinary("payload");
        ret.setPayload(ByteBuffer.wrap(payload));
        if(qos == AbstractMessage.QOSType.LEAST_ONE || qos == AbstractMessage.QOSType.EXACTLY_ONCE) {
            ret.setMessageID(json.getInteger("messageID"));
        }
        return ret;
    }

    public JsonObject serializeWillMessage(String willMsg, byte willQos, String willTopic) {
        JsonObject wm = new JsonObject()
                .put("topicName", willTopic)
                .put("qos", new Integer(willQos))
                .put("message", willMsg);
        return wm;
    }

}
