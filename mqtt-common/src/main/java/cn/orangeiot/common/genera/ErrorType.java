package cn.orangeiot.common.genera;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2017-12-08
 */
public enum  ErrorType {
    RESULT_LOGIN_NO(444,"没有登录"),//没有登录
    RESULT_LOGIN_EMPTY(100, "账号或密码为空"),//登录参数有空值
    RESULT_LOGIN_FIAL(101, "账号或密码错误"),   //登录失败
    RESULT_LOGIN_ILLEGAL(806, "非法数据"),   //数据格式不正确
    RESULT_LOGIN_EVIL(801, "恶意登录"),   //恶意登录
    RESULT_SERVER_FIAL(500, "服务器出小差"),   //服务起出错
    RESULT_RESOURCES_NOT_FIND(404, "资源未找到"),   //资源未找到
    RESULT_UNKONWN(999, "资源未找到"),   //未知异常
    RESULT_SERVER_TIME_OUT(509, "服務請求處理超時"),   //處理超時
    REQUIRED_PRECESSS_FAIL(501, "业务处理失败"),
    RESULT_DATA_FAIL(400, "请求数据或或格式不对"),   //请求数据或或格式不对
    RESULT_PARAMS_FAIL(401, "数据参数不对"),   //必需的参数没有传或参数类型不对null值
    RESULT_CODE_NOREQUIRED(406, "不需要显示验证码"),   //不需要显示验证码
    RESULT_CODE_FAIL(408, "验证码错误"),   //验证码错误
    UPLOAD_FILE_FAIL(607, "上传文件失败"),
    GET_NICKNAME_FAIL(601, "获取昵称失败"),
    REGISTER_USER_FAIL(204, "注册账户失败"),
    UPDATE_USER_PWD_FAIL(208, "修改密码失败"),
    CONTENT_LENGTH_INDEX(301, "字符长度超出限制"),
    CONTENT_SUGGEST_FAIL(302, "留言失败"),
    CODE_COUNT_FAIL(704, "验证码发送次数过多"),
    PASSWORD_FAIL(435, "密码只能数字和字母组合，6-15位"),
    LOGOUT_FAIL(780, "登出失败"),
    ADD_ADMIN_DEV_FAIL(781, "添加设备失败"),
    OPEN_LOCK_FAIL(803, "开锁失败,当前时间没有权限"),
    OPERATION_FAIL(782, "操作失敗"),
    OPEN_LOCK__NOTFAIL(785, "开锁失败！"),
    NOT_BIND(201, "未绑定"),
    BINDED(202, "已绑定");

    private int key;

    private String value;

    ErrorType(int key, String value) {
        this.key = key;
        this.value = value;
    }

    public int getKey() {
        return key;
    }

    public void setKey(int key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }




}
