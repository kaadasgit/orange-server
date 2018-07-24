package cn.orangeiot.mqtt.security.impl;

import java.util.List;

/**
 * Created by giova_000 on 19/02/2015.
 */
class TokenInfo {
    private String authorizedUser;
    private List<String> scope;
    private Long expiryTime;
    private String errorMsg;

    String getAuthorizedUser() {
        return authorizedUser;
    }

    void setAuthorizedUser(String authorizedUser) {
        this.authorizedUser = authorizedUser;
    }

    List<String> getScope() {
        return scope;
    }

    void setScope(List<String> scope) {
        this.scope = scope;
    }

    Long getExpiryTime() {
        return expiryTime;
    }

    void setExpiryTime(Long expiryTime) {
        this.expiryTime = expiryTime;
    }

    String getErrorMsg() {
        return errorMsg;
    }

    void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    @Override
    public String toString() {
        return authorizedUser +" ["+expiryTime+" "+scope+" "+errorMsg+"]";
    }
}
