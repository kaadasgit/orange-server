package cn.orangeiot.mqtt;

import java.util.regex.Pattern;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-11-12
 */
public class SubscriptionTopic {
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
