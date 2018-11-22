package cn.orangeiot.message.constant;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-05-22
 */
public interface ConstantConf {

     String AUTH_PRE = "Basic ";

     String APNS_TOPIC="com.kaidishi.lock";//push通知

     String APNS_TOPIC_VOIP="com.kaidishi.lock.voip";//VIOP通话

     String DEVELOP_API="api.sandbox.push.apple.com";//开发环境

     String PUSH_API="api.push.apple.com";//正式环境
}
