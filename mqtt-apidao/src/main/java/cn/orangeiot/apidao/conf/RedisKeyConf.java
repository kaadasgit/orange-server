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

    public final static String USER_ACCOUNT = "user:";//用户

    public final static String RATE_LIMIT = "limit:";//限流

    public final static String VERIFY_CODE_COUNT = "code:count";//验证码次数

    public final static String USER_INFO = "user:info";//用户信息

    public final static String REGISTER_USER = "sipAcc:";//保存用户注册信息

    public final static String SESSION_BRANCH = "branch:";//会话节点

    public final static String REGISTER_SIP_HEARTS = "sipheart:";//sip心跳包地址

    public final static String CALLIDADDR = "addr:callIdAddr";//保存用户注册信息

    public final static String SAVE_PUBLISH_MSG = "msg:publish";//保存用户注册信息

    public final static String MSG_MODEL_CONF = "msg:conf:";//保存用户注册信息

    public final static String USER_VAL_TOKEN = "token";//token字段

    public final static String BING_USER_INFO = "bindUsers";//绑定的用户

    public final static String USER_VAL_TIMES = "limit";//limit 限流次数字段

    public final static String USER_VAL_INFO = "info";//信息

    public final static String USER_PUSH_ID = "pushId";//應用推送唯一標識

    public final static String USER_VAL_OLDTOKEN = "oldToken";//old token
}
