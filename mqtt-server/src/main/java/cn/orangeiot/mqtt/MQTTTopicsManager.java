package cn.orangeiot.mqtt;

/**
 * Created by giovanni on 10/05/2014.
 * Manages subscritpions and MQTT topic rules
 */
public class MQTTTopicsManager implements ITopicsManager {

    public MQTTTopicsManager() {}

    // 286 millis
    public boolean match(String topic, String topicFilter) {
        if(topicFilter.equals(topic)) {
            return true;
        }
        else {
            if (topicFilter.contains("+") && !topicFilter.endsWith("#")) {
                int topicSlashCount = countSlash(topic);
                int tsubSlashCount = countSlash(topicFilter);
                if (topicSlashCount == tsubSlashCount) {
                    String pattern = toPattern(topicFilter);
                    if (topic.matches(pattern)) {
                        return true;
                    }
                }
            } else if (topicFilter.contains("+") || topicFilter.endsWith("#")) {
                int topicSlashCount = countSlash(topic);
                int tsubSlashCount = countSlash(topicFilter);
                if (topicSlashCount >= tsubSlashCount) {
                    String pattern = toPattern(topicFilter);
                    if (topic.matches(pattern)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // 2 sec
    private String toPattern(String subscribedTopic) {
        String pattern = subscribedTopic;
        pattern = pattern.replaceAll("#", ".*");
        pattern = pattern.replaceAll("\\+", "[^/]*");
        return pattern;
    }

    // 446 millis
    private int countSlash(String s) {
        int count = s.replaceAll("[^/]", "").length();
        return count;
    }

}
