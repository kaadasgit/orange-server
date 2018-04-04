package cn.orangeiot.apidao.conf;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2017-11-23
 */
public class RedisKeyConf {
    public final static String SUBSCRIBE_kEY = "user:subcribe";//mqtt主题key集合

    public final static String SUBSCRIBE_CLIENT_kEY = "topic:";//订阅主题的账户集合

    public final static String USER_OFFLINE_MESSAGE = "offlineMsg:";//用户离线消息

    public final static String USER_ACCOUNT = "user:account";//用户

    public final static String VERIFY_CODE_COUNT = "code:count";//验证码次数

    public final static String USER_INFO = "user:info";//用户信息

    public final static String REGISTER_USER = "register:user";//保存用户注册信息

    public final static String CALLIDADDR = "addr:callIdAddr";//保存用户注册信息

    public final static String SAVE_PUBLISH_MSG = "msg:publish";//保存用户注册信息
}
