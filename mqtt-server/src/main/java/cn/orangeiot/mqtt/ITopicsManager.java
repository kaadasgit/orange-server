package cn.orangeiot.mqtt;

import java.util.Set;

/**
 * Created by giova_000 on 04/03/2015.
 */
public interface ITopicsManager {

//    @Deprecated
//    void addSubscribedTopic(String topic);
//
//    @Deprecated
//    Set<String> calculateTopicsToPublish(String topicOfPublishMessage);

//    @Deprecated
//    Set<String> getSubscribedTopics();

    boolean match(String topic, String topicFilter);

//    @Deprecated
//    void removeSubscribedTopic(String topic);

//    @Deprecated
//    String toVertxTopic(String mqttTopic);

}
