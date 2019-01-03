package cn.orangeiot.common.constant.mongodb;

/**
 * @author : baijun
 * @date : 2018-12-27
 * @description : 订单产品详情信息
 */
public interface KdsProductInfoList {

    String COLLECT_NAME = "kdsProductInfoList";
    String _ID = "_id"; // 主键，唯一性，自动生成

    String SN = "SN"; // 设备号
    String COUNT = "count"; // 数量
    String YEAR_CODE = "yearCode"; // 年代码
    String WEEK_CODE = "weekCode"; // 周代码
    String MODEL_CODE = "modelCode"; // 产品代码
    String FACTORY_CODE = "factoryCode"; // 工厂代码
    String BATCH = "batch"; // 批次
    String TIME = "time"; // 生成时间
    String PASSWORD1 = "password1"; // 秘钥（看具体产品）
    String MAC = "mac"; // mac 地址

    // db no data
    String CHILD_CODE = "childCode"; // 子代码

}
