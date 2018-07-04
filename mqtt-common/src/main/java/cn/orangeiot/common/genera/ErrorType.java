package cn.orangeiot.common.genera;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2017-12-08
 */
public enum ErrorType {
    RESULT_LOGIN_NO(444, "没有登录"),//没有登录
    RESULT_LOGIN_EMPTY(100, "账号或密码为空"),//登录参数有空值
    RESULT_LOGIN_FIAL(101, "账号或密码错误"),   //登录失败
    RESULT_LOGIN_ILLEGAL(806, "非法数据"),   //数据格式不正确
    RESULT_LOGIN_EVIL(801, "恶意登录"),   //恶意登录
    RESULT_SERVER_FIAL(500, "服务器出小差"),   //服务起出错
    RESULT_RESOURCES_NOT_FIND(404, "资源未找到"),   //资源未找到
    RESULT_UNKONWN(999, "资源未找到"),   //未知异常
    RESULT_SERVER_TIME_OUT(509, "服務請求處理超時"),   //處理超時
    REQUIRED_PRECESSS_FAIL(501, "业务处理失败"),
    RESULT_DATA_FAIL(400, "请求数据或格式不对"),   //请求数据或或格式不对
    RESULT_PARAMS_FAIL(401, "数据参数不对"),   //必需的参数没有传或参数类型不对null值
    BODY_SIZE_FAIL(413, "图像体积过大"),   //必需的参数没有传或参数类型不对null值
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
    UPLOAD_PUSHID_FAIL(431, "上傳pushid失敗"),
    ADD_ADMIN_DEV_FAIL(781, "添加设备失败"),
    OPEN_LOCK_FAIL(803, "开锁失败,当前时间没有权限"),
    OPERATION_FAIL(782, "操作失敗"),
    OPEN_LOCK__NOTFAIL(785, "开锁失败！"),
    NOT_BIND(201, "未绑定"),
    BINDED(202, "已绑定"),
    DEV_REGED(409, "设备重复注册"),
    EXCEPTION(401, "要赋权用户没找到！"),
    DEV_REGED_FAIL(411, "要赋权用户没找到！"),
    NO_ADMIN_FAIL(433, "不是管理员"),
    USERNAME_INVALID_FAIL(409, "已经存在用户"),
    DEV_REQUEST_FAIL(412, "设备注册失败 重复的记录！"),
    UPDATE_USER_PREMISSION_FAIL(413, "设备注册失败 重复的记录！"),
    VERIFY_CODE_FAIL(445, "无效的随机码"),
    REGISTER_USER_DICT_FAIL(405, "用户重复注册"),
    UPDATE_NICKNAME_FAIL(601, "修改昵称失敗"),
    PRODUCTION_DEVICESN_FAIL(991, "生产失败"),
    NOTIFY_ADMIN_BY_GATEWAY(812, "已经通知管理员确认"),
    HAVE_ADMIN_BY_GATEWAY(813, "已经绑定了網關"),
    BIND_GATEWAY_FAIL(871, "绑定网关失败"),
    MIMI_BIND_GATEWAY_FAIL(946, "mimi绑定网关失败"),
    APPROVATE_MIMI_BIND_GATEWAY_FAIL(947, "審批mimi绑定网关失败"),
    APPROVAL_FAIL(816, "审批失败"),
    GET_GATEWAY_BIND_FAIL(819, "获取列表失败"),
    GET_GATEWAY_DEVICE_FAIL(820, "获取网关设备列表失败"),
    DATA_MAP_FAIL(436, "数据不是映射关系"),
    SELECT_DATA_NULL(419, "没有数据，参数不匹配"),
    REGISTER_USER_BULK_FAIL(567, "批量注册用户失败"),
    UOLOAD_VERIFY_DATA_MAC_FIAL(592, "SN和password1与mac映射不对"),
    UPDATE_API_FAIL(539, "api接口更新失敗"),
    MQTT_CONNECT_FAIL(407, "mqtt server connect fail"),
    MEMENET_USER_NO_REGISTER(414, "don't register user ,memenet");

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
