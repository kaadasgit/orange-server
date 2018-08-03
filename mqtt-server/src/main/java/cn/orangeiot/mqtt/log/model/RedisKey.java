package cn.orangeiot.mqtt.log.model;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-07-25
 */
public interface RedisKey {

    String LOG_RECODR="logRecord:";//消息記錄key

    String LOG_PUBREL="logPubRel:";//消息記錄释放的rel id

}
