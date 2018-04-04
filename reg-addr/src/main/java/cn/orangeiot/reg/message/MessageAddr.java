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

    String SAVE_OFFLINE_MSG = "saveOfflineMessage";//存储离线消息

}
