package cn.orangeiot.common.constant;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2017-12-08
 */
public enum HttpAttrType {
    CONTENT_TYPE_HTML("content-type", "text/html"),   //HTML格式
    CONTENT_TYPE_PLAIN("content-type", "text/plain"),  //纯文本格式
    CONTENT_TYPE_XML("content-type", "text/xml"),    // XML格式
    CONTENT_TYPE_JSON("content-type", "application/json"),//JSON数据格式
    CONTENT_TYPE_FORM_DATA("content-type", "multipart/form-data");//文件上传

    private String key;
    private String value;

    HttpAttrType(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}