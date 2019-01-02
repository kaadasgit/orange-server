package cn.orangeiot.common.constant.mongodb;

/**
 * @author : baijun
 * @date : 2018-12-27
 */
public interface KdsUser {

    String COLLECT_NAME = "kdsUser";
    String _ID = "_id"; // 主键

    String _CLASS = "_class";
    String USER_TEL = "userTel"; // 用户电话
    String USER_PWD = "userPwd"; // 用户密码
    String NICK_NAME = "nickName"; // 昵称
    String PWD_SALT = "pwdSalt"; //加密盐 MD5,旧版加密逻辑
    String ME_USERNAME = "meUsername"; // 第三方米米网用户
    String ME_PWD = "mePwd"; // 第三方米米网密码
    String USER_ID = "userid"; // 第三方米米网用户id
    String VERSION_TYPE = "versionType"; // 用户应用区分标识（kaadas、orangelet、PHILIPS、STAYLOCK、vendors）

    String USER_MAIL = "userMail"; // 用户邮箱

    // data not in db
    String USER_GW_ACCOUNT = "userGwAccount";
    String INSERT_TIME = "insertTime";

}
