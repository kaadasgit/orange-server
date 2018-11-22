package cn.orangeiot.reg.message;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2017-12-12
 */
public interface MessageAddr {

    String SAVE_CODE = "saveCode";//保存验证码

    String GET_CODE_COUNT = "getCodeCount";//获取验证码次数

    String NOTIFY_GATEWAY_USER_ADMIN = "notifyGatewayAdmin";//通知网关管理员

    String REPLY_GATEWAY_USER = "replyGatewayUser";//回复网关绑定申请用户

    String GET_GATEWAY_ADMIN = "getGatewayAdmin";//获取网关管理员

    String SEND_ADMIN_MSG = "sendAdminMsg";//发送mqtt消息

    String SEND_USER_REPLAY = "/clientId/rpc/reply";//回复app

    String SEND_GATEWAY_REPLAY = "/orangeiot/gwId/call";//回复网关

    String SAVE_OFFLINE_MSG = "saveOfflineMessage";//存储离线消息

    String SEND_UPGRADE_MSG = "sendGatewayMsg";//發送網關消息

    String SEND_VERSION_UPGRADE_MSG = "sendUpgradeMsg";//發送升級消息

    String SEND_STORAGE_MSG = "sendStorageMsg";//發送存儲消息

    String SEND_PUBREL_MSG = "sendPubRelMsg";//發送REL釋放消息

    String SEND_APPLICATION_NOTIFY = "sendAppNotify";//發送應用通知

    String SEND_APPLICATION_SOUND_NOTIFY = "sendAppSoundNotify";//發送音响通知

    String GET_PUSHID = "getPushId";//获取应用id

    String KICK_OUT = "kickOut";//踢出
}