package cn.orangeiot.common.constant.mongodb;

/**
 * @author : baijun
 * @date : 2018-12-27
 * @description : 用户留言表，用于记录用户留言，和kdsUser外键关联（uid）
 */
public interface KdsSuggest {

    String COLLECT_NAME = "kdsSuggest";
    String _ID = "_id"; // 主键，唯一性，自动生成

    String _CLASS = "_class";
    String UID = "uid"; // 用户id(kdsUser外键)
    String TOKENS = "tokens";
    String SUGGEST = "suggest"; // 留言内容（字符小于30）

    String TIME = "time";
}
