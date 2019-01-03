package cn.orangeiot.common.constant.mongodb;

/**
 * @author : baijun
 * @date : 2018-12-27
 * @description : 开锁记录信息，和kdsDeviceList外键相连（外键：lockName）
 */
public interface KdsOpenLockList {

    String COLLECT_NAME = "kdsOpenLockList";
    String _ID = "_id"; // 主键唯一性，

    String LOCK_NAME = "lockName"; // 锁设备名称（唯一），外键
    String MEDIUM = "medium"; // 媒介物
    String OPEN_TIME = "open_time"; // 开锁时间
    String OPEN_TYPE = "open_type"; // 开锁类型(指纹、打卡、密码)
    String UID = "uid"; // 用户id
    String NICK_NAME = "nickName"; // 开锁用户昵称
    String ADMIN_UID = "adminuid"; // 管理员用户id
    String USER_NUM = "user_num"; // 用户类型
	// db no data
	String VERSION_TYPE = "versionType"; // 用户应用区分标识
	String LOCK_NICK_NAME = "lockNickName"; // 锁设备昵称
	String U_NAME = "uname"; // 开锁用户账户
	String OPEN_PURVIEW = "open_purview"; // 锁权限类型（1：一次性开锁，2：时间段开锁，3：无限开锁）
	String AUTH = "auth";

}
