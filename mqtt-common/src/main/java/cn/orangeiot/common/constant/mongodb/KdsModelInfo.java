package cn.orangeiot.common.constant.mongodb;

/**
 * @author : baijun
 * @date : 2018-12-27
 * @description : 生产订单产品
 */
public interface KdsModelInfo {

    String COLLECT_NAME = "kdsModelInfo";
    String _ID = "_id"; // 主键 唯一性，自动生成

    String YEAR_CODE = "yearCode"; // 年代码
    String WEEK_CODE = "weekCode"; // 周代码
    String MODEL_CODE = "modelCode"; // 产品代码
    String CHILD_CODE = "childCode"; // 子代码
    String COUNT = "count"; // 生产数量
    String TIME = "time"; // 生产时间

}
