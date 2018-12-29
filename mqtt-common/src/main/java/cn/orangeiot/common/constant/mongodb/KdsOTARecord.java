package cn.orangeiot.common.constant.mongodb;

/**
 * @author : baijun
 * @date : 2018-12-27
 * @description : ota设备升级相关记录 和 kdsOtaUpgrade 外键相连（外键：OTAOrderNo）
 */
public interface KdsOTARecord {

    String COLLECT_NAME = "kdsOTARecord";
    String _ID = "_id"; // 主键唯一性，自动生成

    String UID = "uid"; // 如果用户确认，审批 id
    String DEVICE_LIST = "deviceList"; // 升级的设备
    String TYPE = "type"; // 类型（1：确认升级 2 ： 拒绝升级）
    String FILE_URL = "fileUrl"; // 升级固件地址
    String SW = "SW"; // 版本
    String TIME = "time"; // 时间

}
