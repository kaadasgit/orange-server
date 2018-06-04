package cn.orangeiot.reg.adminlock;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2017-12-22
 */
public interface AdminlockAddr {

    String CREATE_ADMIN_DEV = "createadmindev";//添加设备

    String DELETE_EVEND_DEV = "deletevendordev";//第三方重置设备

    String DELETE_ADMIN_DEV = "deleteadmindev";//用户主动删除设备

    String DELETE_NORMAL_DEV = "deletenormaldev";//管理员删除用户

    String CREATE_NORMAL_DEV = "createNormalDev";//管理员为设备添加普通用户

    String GET_OPEN_LOCK_RECORD = "downloadopenlocklist";//获取开锁记录

    String UPDATE_USER_PREMISSON = "updateNormalDevlock";//管理员修改普通用户权限

    String REQUEST_USER_OPEN_LOCK = "adminOpenLock";//用户申请开锁

    String GET_DEV_LIST = "getAdminDevlist";//获取设备列表

    String LOCK_AUTH = "lockAuth";//開鎖講權

    String GET_DEV_USER_LIST = "getNormalDevlist";//设备下的普通用户列表

    String EDIT_ADMIN_DEV = "editadmindev";//管理员修改锁的位置信息

    String GET_DEV_LONGTITUDE = "getAdminDevlocklongtitude";//获取设备经纬度等信息

    String UPDATE_ADMIN_DEV_AUTO_LOCK = "updateAdminDevAutolock";//修改设备是否开启自动解锁功能

    String UPDATE_DEV_NICKNAME = "updateAdminlockNickName";//修改设备昵称

    String CHECK_DEV = "checkadmindev";//检测是否被绑定

    String UPLOAD_OPEN_LOCK_RECORD = "uploadopenlocklist";//上传开门记录

    String MODEL_PRODUCT = "modelProduct";//模块生产

    String MODEL_MAC_IN = "modelMacIn";//mac地址写入

    String MODEL_MANY_MAC_IN = "modelManyMacIn";//mac地址多写入

    String GET_WRITE_MAC_RESULT = "getWriteMacResult";//获取mac写入结果

    String GET_MODEL_PASSWORD = "getPwdByMac";//根据mac获取模块的password1
}
