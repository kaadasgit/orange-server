package cn.orangeiot.reg.user;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2017-12-12
 */
public interface UserAddr {

     String LOGIN_TEL="loginTel";//手机登录

     String LOGIN_MAIL="loginmail";//邮箱登录

     String VERIFY_LOGIN="verify";//检查是否登录

     String REGISTER_USER_TEL="registerTel";//手机注册

     String REGISTER_USER_MAIL="registerMail";//邮箱注册

     String SMS_CODE="smsCode";//短信验证码

     String MAIL_CODE="emailCode";//邮箱验证码

     String VERIFY_TEL="verifyTel";//验证手机登录

     String VERIFY_MAIL="verifyMail";//验证邮箱登录

     String GET_USER_NICKNAME="getNickname";//获取用户昵称

     String UPDATE_USER_NICKNAME="updateNickname";//修改用户昵称

     String UPDATE_USER_PWD="updateUserPwd";//修改用户密码

     String FORGET_USER_PWD="forgetUserPwd";//忘记用户密码

     String SUGGEST_MSG="suggestMsg";//用户留言

     String USER_LOGOUT="logout";//用户登出

     String UPLOAD_JPUSHID="uploadPushId";//上報pushId

     String MEME_USER="memeUser";//同步米米用户信息

     String MEME_REGISTER_USER_BULK="memeRegisterUserBulk";//批量注册用户

     String SAVE_REGISTER_USER="saveRegisterUser";//注冊用戶信息

     String HEARTBEAT_REGISTER_USER="heartbeatRegisterUser";//注冊用戶信息心跳包

     String GET_REGISTER_USER="getRegisterUser";//獲取用戶信息

     String DEL_REGISTER_USER="delRegisterUser";//刪除用戶信息

     String SAVE_CALL_ID="saveCallIdAddr";//會話話的用戶地址映射

     String GET_CALL_ID="getCallIdAddr";//獲取用戶地址映射

     String SAVE_SESSION_BRANCH="saveSessionBranch";//保存会话节点

     String GET_SESSION_BRANCH="getSessionBranch";//获取会话节点

     String REMOVE_SESSION_BRANCH="removeSessionBranch";//移除会话节点

     String GET_GW_ADMIN="getGatewayAdmin";//獲取網關管理員
}
