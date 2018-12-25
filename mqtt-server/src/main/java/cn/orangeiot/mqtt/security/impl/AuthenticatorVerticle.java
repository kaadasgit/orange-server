package cn.orangeiot.mqtt.security.impl;

import io.vertx.core.AbstractVerticle;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * Created by giova_000 on 15/10/2015.
 */
public abstract class AuthenticatorVerticle extends AbstractVerticle {

    protected Logger logger = LogManager.getLogger(getClass());

    @Override
    public void start() throws Exception {

        String address = config().getString("address", this.getClass().getName());
        if(address!=null && address.trim().length()>0) {
            AuthenticatorConfig c = new AuthenticatorConfig(config());
            startAuthenticator(address, c);
        }

    }

    public abstract void startAuthenticator(String address, AuthenticatorConfig config) throws Exception;
}
