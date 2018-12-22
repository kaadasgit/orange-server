package cn.orangeiot.publish.model;

import java.io.Serializable;

/**
 * @author zhang bo
 * @version 1.0
 * @Description  接口层统一返回的数据封装接口
 * @date 2017-12-22
 */
public class ResultInfo<E> implements Serializable {
    public final static String CODE_SUCCESS = "200";
    public String code = CODE_SUCCESS;
    public String msg;
    public String func;
    //public String sign;//签名,使用于数据安全验证码

    private E data;

    public ResultInfo() {
        this.code = CODE_SUCCESS;
        this.func = "";
        this.msg = "成功";
    }

    public ResultInfo(E data,String func) {
        this.code = CODE_SUCCESS;
        this.msg = "成功";
        this.data = data;
        this.func = func;
    }

    public String getFunc() {
        return this.func;
    }

    public ResultInfo<E> setFunc(String func) {
        this.func = func;
        return this;
    }

    public ResultInfo<E> setData(E data) {
        this.data = data;
        return this;
    }

    public E getData() {
        return this.data;
    }

    public String getCode() {
        return this.code;
    }

    public ResultInfo<E> setCode(int code) {
        this.code = String.valueOf(code);
        return this;
    }

    public String getMsg() {
        return this.msg;
    }

    public ResultInfo<E> setMsg(String msg) {
        this.msg = msg;
        return this;
    }


    public ResultInfo<E> setErrorMessage(int code, String errorMsg,String func) {
        this.code = String.valueOf(code);
        this.msg = errorMsg;
        this.func = func;
        return this;
    }



/*	@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    private Page page;
	public Page getPage() {
		return page;
	}

	public void setPage(Page page) {
		this.page = page;
	}*/
}
