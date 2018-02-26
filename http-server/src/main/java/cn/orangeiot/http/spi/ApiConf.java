package cn.orangeiot.http.spi;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2017-12-08
 */
public interface ApiConf {

    /**
     * api路径
     */
    String USER_LOGIN_TEL = "/user/login/getuserbytel";//用户手机登录

    String USER_LOGIN_MAIL = "/user/login/getuserbymail";//用户邮箱登录

    String USER_REGISTER_TEL = "/user/reg/putuserbytel";//用户手机号注册

    String USER_REGISTER_MAIL = "/user/reg/putuserbyemail";//用户邮箱注册

    String USER_LOGOUT = "/user/logout";//登出

    String SEND_SMS_CODE = "/sms/sendSmsTokenByTX";//发送手机验证码

    String SEND_EMAIL_CODE = "/mail/sendemailtoken";//发送邮箱验证码

    String GET_FILE_HEADER_IMG = "/user/edit/showfileonline/:uid";//获取头像

    String UPLOAD_HEADER_IMG = "/user/edit/uploaduserhead";//上传头像

    String USER_NICKNAME = "/user/edit/getUsernickname";//获取用户昵称

    String UPDATE_NICKNAME = "/user/edit/postUsernickname";//修改用户昵称

    String UPDATE_PASSWORD = "/user/edit/postUserPwd";//修改用户密码

    String FORGET_PASSWORD = "/user/edit/forgetPwd";//忘记密码

    String SUGGEST_MSG = "/suggest/putmsg";//用户留言

    String CREATE_ADMIN_DEV = "/adminlock/reg/createadmindev";//添加设备

    String DELETE_EVEND_DEV = "/adminlock/reg/deletevendordev";//第三方重置设备

    String DELETE_ADMIN_DEV = "/adminlock/reg/deleteadmindev";//用户主动删除设备

    String DELETE_NORMAL_DEV = "/normallock/reg/deletenormaldev";//管理员删除用户

    String CREATE_NORMAL_DEV = "/normallock/reg/createNormalDev";//管理员为设备添加普通用户

    String GET_OPEN_LOCK_RECORD = "/openlock/downloadopenlocklist";//获取开锁记录

    String UPDATE_USER_PREMISSON = "/normallock/ctl/updateNormalDevlock";//管理员修改普通用户权限

    String REQUEST_USER_OPEN_LOCK = "/adminlock/open/adminOpenLock";//用户申请开锁

    String GET_DEV_LIST = "/adminlock/edit/getAdminDevlist";//获取设备列表

    String GET_DEV_USER_LIST = "/normallock/ctl/getNormalDevlist";//设备下的普通用户列表

    String EDIT_ADMIN_DEV = "/adminlock/edit/editadmindev";//管理员修改锁的位置信息

    String GET_DEV_LONGTITUDE = "/adminlock/edit/getAdminDevlocklongtitude";//获取设备经纬度等信息

    String UPDATE_ADMIN_DEV_AUTO_LOCK = "/adminlock/edit/updateAdminDevAutolock";//修改设备是否开启自动解锁功能

    String UPDATE_DEV_NICKNAME = "/adminlock/edit/updateAdminlockNickName";//修改设备昵称

    String CHECK_DEV = "/adminlock/edit/checkadmindev";//检测是否被绑定

    String UPLOAD_OPEN_LOCK_RECORD = "/openlock/uploadopenlocklist";//上传开门记录

    String MODEL_PWD_BY_MAC = "/model/getpwdBymac";//根据mac获取password1
}
