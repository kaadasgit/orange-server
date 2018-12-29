package cn.orangeiot.common.constant.mongodb;

/**
 * @author : baijun
 * @date : 2018-12-27
 * @description : 锁设备列表信息
 */
public interface KdsDeviceList {

    String COLLECT_NAME = "kdsDeviceList";
    String _ID = "_id"; // 主键，唯一性，自动生成
    String LOCK_NAME = "lockName"; // 锁设备名称(唯一)
    String U_NAME = "uname"; // 管理员名称
    String INFO_LIST = "infoList"; // 信息列表
    String INFO_LIST_NUM = "num";
    String INFO_LIST_NUM_NICK_NAME = "numNickname";
    String INFO_LIST_TIME = "time";
    String MODEL = "model";
}
