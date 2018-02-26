package cn.orangeiot.apidao.jwt;

import io.vertx.core.Vertx;
import io.vertx.core.cli.CLI;
import io.vertx.core.cli.CommandLine;
import io.vertx.core.cli.Option;
import io.vertx.ext.auth.KeyStoreOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;

import java.util.Arrays;
import java.util.List;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2017-12-29
 */
public class JwtFactory {

    /**
     * JWT config
     *
     * @param vertx
     * @return
     */
    public JWTAuth JWTConf(Vertx vertx, String args) {
        JWTAuth jwt = JWTAuth.create(vertx, new JWTAuthOptions()
                .setKeyStore(new KeyStoreOptions().setPath(args).setType("jceks").setPassword("secret")));
        return jwt;
    }

}
