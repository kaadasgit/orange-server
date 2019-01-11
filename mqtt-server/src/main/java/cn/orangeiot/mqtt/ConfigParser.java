package cn.orangeiot.mqtt;

import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Created by giova_000 on 23/02/2015.
 */
public class ConfigParser {

    private int port;
    private boolean wsEnabled;
    private String wsSubProtocols;
    private boolean retainSupport;
    private String authenticatorAddress;
    private boolean securityEnabled;
    private String tlsPassword;
    private String tlsCertPath;
    private String publish;
    private int socketIdleTimeout;
    private String reply_message;
    private String dirPath;
    private int segmentSize;
    private long expireTime;
    private long periodicTime;
    private String user_prefix;
    private String gateway_prefix;

    public long getPeriodicTime() {
        return periodicTime;
    }

    private static final int IDLE_TIMEOUT_SECONDS = 120;

    public ConfigParser() {
    }

    public ConfigParser(JsonObject conf) {
        parse(conf);
    }

    public long getExpireTime() {
        return expireTime;
    }

    public void parse(JsonObject conf) {
        port = conf.getInteger("tcp_port", 1883);
        wsEnabled = conf.getBoolean("websocket_enabled", false);
        wsSubProtocols = conf.getString("websocket_subprotocols", "mqtt,mqttv3.1");
        retainSupport = conf.getBoolean("retain_support", true);

        authenticatorAddress = conf.getString("authenticator_address");
        securityEnabled = authenticatorAddress != null && authenticatorAddress.trim().length() > 0;

        JsonObject tls = conf.getJsonObject("tls", new JsonObject());
        tlsPassword = tls.getString("password");
        tlsCertPath = tls.getString("certPath");
        socketIdleTimeout = conf.getInteger("socket_idle_timeout", IDLE_TIMEOUT_SECONDS);
        publish = conf.getString("send_publish_message");

        if (Objects.nonNull(conf.getValue("dirPath")))
            dirPath = conf.getString("dirPath");
        if (Objects.nonNull(conf.getValue("segmentSize")))
            segmentSize = conf.getInteger("segmentSize");
        if (Objects.nonNull(conf.getValue("expireTime")))
            expireTime = conf.getLong("expireTime");
        if (Objects.nonNull(conf.getValue("periodicTime")))
            periodicTime = conf.getLong("periodicTime");
        if (Objects.nonNull(conf.getValue("user_prefix")))
            user_prefix = conf.getString("user_prefix");
        if (Objects.nonNull(conf.getValue("gateway_prefix")))
            gateway_prefix = conf.getString("gateway_prefix");
    }

    public String getUser_prefix() {
        return user_prefix;
    }

    public String getGateway_prefix() {
        return gateway_prefix;
    }

    public String getReply_message() {
        return reply_message;
    }

    public String getPublish() {
        return publish;
    }

    public void setPublish(String publish) {
        this.publish = publish;
    }

    public int getPort() {
        return port;
    }

    public boolean isWsEnabled() {
        return wsEnabled;
    }

    public String getWsSubProtocols() {
        return wsSubProtocols;
    }

    public String getAuthenticatorAddress() {
        return authenticatorAddress;
    }

    public boolean isSecurityEnabled() {
        return securityEnabled;
    }

    public String getTlsPassword() {
        return tlsPassword;
    }

    public String getTlsCertPath() {
        return tlsCertPath;
    }

    public boolean isTlsEnabled() {
        String keyPath = getTlsPassword();
        String certPath = getTlsCertPath();
        boolean ret = keyPath != null && keyPath.trim().length() > 0 && certPath != null && certPath.trim().length() > 0;
        return ret;
    }

    public boolean isRetainSupport() {
        return retainSupport;
    }

    public List<String> getFeatures() {
        List<String> ret = new ArrayList<>();
        if (wsEnabled)
            ret.add("websocket");
        if (securityEnabled)
            ret.add("security");
        if (!retainSupport)
            ret.add("retain_disabled");
        return ret;
    }

    public String getFeatursInfo() {
        StringBuilder ret = new StringBuilder();
        List<String> features = getFeatures();
        for (String f : features) {
            ret.append(f).append(",");
        }
        String s = ret.toString();
        int idx = s.lastIndexOf(',');
        if (idx >= 0)
            s = s.substring(0, idx);
        return s;
    }

    public String getDirPath() {
        return dirPath;
    }

    public int getSegmentSize() {
        return segmentSize;
    }

    public int getSocketIdleTimeout() {
        return socketIdleTimeout;
    }
}
