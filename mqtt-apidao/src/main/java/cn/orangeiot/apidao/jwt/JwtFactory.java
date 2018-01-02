package cn.orangeiot.apidao.jwt;

import io.vertx.core.Vertx;
import io.vertx.ext.auth.KeyStoreOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;

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
    public JWTAuth JWTConf(Vertx vertx) {
        String path = JwtFactory.class.getResource("/keystore.jceks").getPath();
        // Create a JWT Auth Provider
        JWTAuth jwt = JWTAuth.create(vertx, new JWTAuthOptions()
                .setKeyStore(new KeyStoreOptions().setPath(path).setType("jceks").setPassword("secret")));
        return jwt;
    }
}
