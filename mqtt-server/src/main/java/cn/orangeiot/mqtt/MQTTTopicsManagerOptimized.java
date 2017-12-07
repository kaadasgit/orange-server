package cn.orangeiot.mqtt;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Created by giovanni on 10/05/2014. Manages subscritpions and MQTT topic rules
 */
public class MQTTTopicsManagerOptimized implements ITopicsManager {
    public static class SubscriptionTopic {
        private String topic;
        private Pattern regexPattern;

        public SubscriptionTopic(String topic) {
            this.topic = topic;
        }

        public Pattern getRegexPattern() {
            return regexPattern;
        }

        public void setRegexPattern(Pattern regexPattern) {
            this.regexPattern = regexPattern;
        }

        public String getTopic() {
            return topic;
        }

        public void setTopic(String topic) {
            this.topic = topic;
        }
    }

    private Map<String, SubscriptionTopic> topicsSubscribedMap = new LinkedHashMap<String, SubscriptionTopic>();

    /**
     * jvisualvm 46millis cpu profile
     * @param topic
     * @param topicFilter
     * @return
     */
    public boolean match(String topic, String topicFilter) {
        SubscriptionTopic st = createSubscriptionTopic(topicFilter);
        Pattern tregex = st.getRegexPattern();

        boolean match = tregex.matcher(topic).matches();
        return match;
    }

    private SubscriptionTopic createSubscriptionTopic(String topic) {
        SubscriptionTopic st = topicsSubscribedMap.get(topic);
        if (st == null) {
            st = new SubscriptionTopic(topic);
            st.setRegexPattern(toRegexPattern(topic));

            topicsSubscribedMap.put(topic, st);
        }
        return st;
    }

    private Pattern toRegexPattern(String subscribedTopic) {
        String regexPattern = subscribedTopic;
        regexPattern = regexPattern.replaceAll("#", ".*");
        regexPattern = regexPattern.replaceAll("\\+", "[^/]*");
        Pattern pattern = Pattern.compile(regexPattern);
        return pattern;
    }

}

