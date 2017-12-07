package cn.orangeiot.mqtt;

import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

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
    private String tlsKeyPath;
    private String tlsCertPath;
    private int socketIdleTimeout;

    private static final int IDLE_TIMEOUT_SECONDS = 120;

    public ConfigParser() { }

    public ConfigParser(JsonObject conf) {
        parse(conf);
    }
    public void parse(JsonObject conf) {
        port = conf.getInteger("tcp_port", 1883);
        wsEnabled = conf.getBoolean("websocket_enabled", false);
        wsSubProtocols = conf.getString("websocket_subprotocols", "mqtt,mqttv3.1");
        retainSupport = conf.getBoolean("retain_support", true);

        authenticatorAddress = conf.getString("authenticator_address");
        securityEnabled = authenticatorAddress!=null && authenticatorAddress.trim().length()>0;

        JsonObject tls = conf.getJsonObject("tls", new JsonObject());
        tlsKeyPath = tls.getString("keyPath");
        tlsCertPath = tls.getString("certPath");
        socketIdleTimeout = conf.getInteger("socket_idle_timeout", IDLE_TIMEOUT_SECONDS);
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

    public String getTlsKeyPath() {
        return tlsKeyPath;
    }

    public String getTlsCertPath() {
        return tlsCertPath;
    }

    public boolean isTlsEnabled() {
        String keyPath = getTlsKeyPath();
        String certPath = getTlsCertPath();
        boolean ret = keyPath!=null && keyPath.trim().length()>0 && certPath!=null && certPath.trim().length()>0;
        return ret;
    }

    public boolean isRetainSupport() {
        return retainSupport;
    }

    public List<String> getFeatures() {
        List<String> ret = new ArrayList<>();
        if(wsEnabled)
            ret.add("websocket");
        if(securityEnabled)
            ret.add("security");
        if(!retainSupport)
            ret.add("retain_disabled");
        return ret;
    }
    public String getFeatursInfo() {
        StringBuilder ret = new StringBuilder();
        List<String> features = getFeatures();
        for(String f : features) {
            ret.append(f).append(",");
        }
        String s = ret.toString();
        int idx = s.lastIndexOf(',');
        if(idx>=0)
            s = s.substring(0, idx);
        return s;
    }

    public int getSocketIdleTimeout() {
        return socketIdleTimeout;
    }
}
