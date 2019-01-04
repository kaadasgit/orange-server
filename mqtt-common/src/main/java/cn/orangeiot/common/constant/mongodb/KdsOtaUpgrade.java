package cn.orangeiot.common.constant.mongodb;

/**
 * @author : baijun
 * @date : 2018-12-27
 */
public interface KdsOtaUpgrade {

    String COLLECT_NAME = "kdsOtaUpgrade";
    String _ID = "_id"; // 主键，唯一性，自动生成

    String MODEL_CODE = "modelCode"; // 产品代号
    String CHILD_CODE = "childCode"; // 子代码
    String WEEK_CODE = "weekCode"; // 周代码
    String YEAR_CODE = "yearCode"; // 年代码
    String TYPE = "type"; // 升级类型（0：强制升级 1：用户确认升级）
    String RANGE = "range"; // 升级范围
    String TIME = "time"; // 提交时间
    String FILE_PATH_URL = "filePathUrl";
    String MODEL_TYPE = "modelType"; // 升级的产品类型（1：网关 2：挂载设备）
    String FILE_MD5 = "fileMd5";//文件md5
    String FILE_LEN = "fileLen";//文件愛呢長度
    String SW = "SW";//版本

    // no data in db
    String OTA_ORDER_NO = "OTAOrderNo"; // ota 单号 唯一性
}
