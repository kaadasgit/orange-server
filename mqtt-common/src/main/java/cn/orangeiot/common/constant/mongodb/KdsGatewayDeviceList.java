package cn.orangeiot.common.constant.mongodb;

/**
 * @author : baijun
 * @date : 2018-12-27
 * @description : 网关设备信息
 */
public interface KdsGatewayDeviceList {

    String COLLECT_NAME = "kdsGatewayDeviceList";
    String _ID = "_id"; // 主键唯一性 自动生成

    String DEVICE_SN = "deviceSN"; // 设备 SN
    String UID = "uid"; // 用户id
    String DEVICE_NICK_NAME = "deviceNickName"; // 设备昵称
    String USER_NAME = "username"; // 用户账户
    String USER_NICK_NAME = "userNickname"; // 用户昵称
    String ADMIN_UID = "adminuid"; // 管理员 uid
    String ADMIN_NAME = "adminName"; // 管理员 账户
    String ADMIN_NICK_NAME = "adminNickname"; // 管理员昵称
    String IS_ADMIN = "isAdmin"; // 是否是管理员 1 管理员
    String BIND_TIME = "bindTime"; // 绑定时间
    String USER_ID = "userid"; // 第三方米米用户 id
    String DOMAIN = "domain"; // 第三方米米网域
    String DEVICE_LIST = "deviceList"; // 网关挂载设备相关信息状态

    // 设备列表下面的二级键
    String _DL_IP_ADDR = "ipaddr";
    String _DL_MAC_ADDR = "macaddr";
    String _DL_SW = "SW";
    String _DL_IEEE_ADDR = "ieeeaddr";
    String _DL_NW_ADDR = "nwaddr";
    String _DL_EVENT_STR = "delete";
    String _DL_DEVICE_TYPE = "net";
    String _DL_DEVICE_ID = "deviceId";
    String _DL_TIME = "time";

    String ME_USER_NAME = "meUsername";
    String ME_PWD = "mePwd";
    String ME_BIND_STATE = "meBindState";


}
