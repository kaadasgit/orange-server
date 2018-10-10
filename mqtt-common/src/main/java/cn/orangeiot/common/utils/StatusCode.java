package cn.orangeiot.common.utils;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-08-29
 */
public interface StatusCode {
    //服務器錯誤
    int SERVER_ERROR = 500;

    //请求参数有误,语义有误
    int BAD_REQUEST = 400;

    //请求失败，资源未被找到或不存在
    int Not_FOUND = 404;

    //请求数据大小超过bodylimit
    int REQUEST_TOO_LARGE = 413;

    //服务器处理超时
    int SERVER_TIMEOUT = 504;

    //用户验证失败,或没有header token字段
    int UNAUTHORIZED = 401;

    //参数不对
    int PARAMS_FIAL = 406;

    //不是系統賬戶
    int EXCEPTION_SYSTEM_ACCOUNT = 999;

    //错误的主题,不支持
    int FAIL_TOPCINAME = 946;

    //暂时不存在的异常
    int NO_EXISTS_EX = 599;

    //成功
    int SUCCESSS = 200;
}
