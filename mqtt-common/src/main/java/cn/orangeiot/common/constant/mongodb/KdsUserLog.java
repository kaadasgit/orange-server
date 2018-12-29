package cn.orangeiot.common.constant.mongodb;
/**
 * @author : baijun
 * @date : 2018-12-27
 * @description : 用户日志表，存储用户操作日志
 */
public interface KdsUserLog {

    String COLLECT_NAME = "kdsUserLog";
    String _ID = "_id"; // 主键id,唯一性，自动生成

    String _CLASS = "_class";
    String USER_NAME = "userName"; //用户账户
    String LOGIN_TIME = "loginTime"; // 登录时间
    String LOGIN_IP = "loginIp"; // 登录地址
    String REQUEST_STR = "requeststr"; // 请求消息
    String VERSION_TYPE = "versionType"; // 用户应用区分标识

}
