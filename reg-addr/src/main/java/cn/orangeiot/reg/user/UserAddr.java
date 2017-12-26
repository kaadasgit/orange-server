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
}
