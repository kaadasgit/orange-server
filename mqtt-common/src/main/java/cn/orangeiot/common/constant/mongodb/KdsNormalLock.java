package cn.orangeiot.common.constant.mongodb;

/**
 * @author : baijun
 * @date : 2018-12-27
 * @description : 锁与设备关系集合，与kdsDeviceList 外键相连(外键：lockName)
 */
public interface KdsNormalLock {

    String COLLECT_NAME = "kdsNormalLock";
    String _ID = "_id"; // 主键，唯一性，自动生成

    String LOCK_NAME = "lockName"; // 锁设备名称，外键
    String LOCK_NICK_NAME = "lockNickName"; // 锁设备昵称
    String MAC_LOCK = "macLock"; // mac 地址
    String ADMIN_UID = "adminuid"; // 管理员用户id(外键)
    String ADMIN_NAME = "adminname"; // 管理员用户名
    String ADMIN_NICK_NAME = "adminnickname"; // 管理员昵称
    String UID = "uid"; // 当前绑定用户 id (外键)
    String U_NAME = "uname"; // 当前绑定用户名
    String U_NICK_NAME = "unickname"; // 当前绑定用户昵称
    String OPEN_PURVIEW = "open_purview"; // 锁权限类型（1：一次性开锁，2：时间段开锁，3：无限开锁)
    String IS_ADMIN = "is_admin"; // 是否是管理员（1 ： 管理员）
    String DATE_START = "datestart"; // 开始时间
    String DATE_END = "dateend"; // 结束时间
    String CENTER_LATITUDE = "center_latitude"; // 中心维度
    String CENTER_LONGITUDE = "center_longitude"; // 中心经度
    String EDGE_LATITUDE = "edge_latitude"; // 边维度
    String EDGE_LONGITUDE = "edge_longitude"; // 边经度
    String CIRCLE_RADIUS = "circle_radius"; // 圈中心率
    String AUTO_LOCK = "auto_lock"; // 是否自动开锁
    String ITEMS = "items"; // 一周，size 7
    String PASSWORD1 = "password1"; // 开锁秘钥1
    String PASSWORD2 = "password2"; // 开锁秘钥2
    String VERSION_TYPE = "versionType"; // 用户应用区分
    String MODEL = "model";

}
