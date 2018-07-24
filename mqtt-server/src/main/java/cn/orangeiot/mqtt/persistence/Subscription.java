package cn.orangeiot.mqtt.persistence;

import cn.orangeiot.mqtt.QOSUtils;
import io.vertx.core.json.JsonObject;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage;

/**
 * Created by giovanni on 21/05/2014.
 */
public class Subscription {
    private String topicFilter;
    private int qos;

    public String getTopicFilter() {
        return topicFilter;
    }

    public void setTopicFilter(String topicFilter) {
        this.topicFilter = topicFilter;
    }

    public int getQos() {
        return qos;
    }

    public void setQos(int qos) {
        this.qos = qos;
    }

    private JsonObject toJson() {
        JsonObject s = new JsonObject()
                .put("topicFilter", this.topicFilter)
                .put("qos", this.qos);
        return s;
    }

    private void fromJson(JsonObject json) {
        int qos = json.getInteger("qos", 0);
        AbstractMessage.QOSType qosType = new QOSUtils().toQos(qos);
        this.qos = new QOSUtils().toByte(qosType);
        this.topicFilter = json.getString("topicFilter");
    }

    public void fromString(String s) {
        JsonObject json = new JsonObject(s);
        fromJson(json);
    }
    public String toString() {
        String s = toJson().encode();
        return s;
    }

}
