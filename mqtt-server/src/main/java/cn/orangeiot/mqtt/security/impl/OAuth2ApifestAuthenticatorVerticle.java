package cn.orangeiot.mqtt.security.impl;

import cn.orangeiot.mqtt.security.AuthorizationClient;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.json.JsonObject;

import java.net.URL;

/**
 * Created by Giovanni Baleani on 04/02/2015.
 */

/**
 GET http://127.0.0.1:8080/oauth20/tokens/validate?token=f48db3829c71b9dc4957e3bb7b804bd0d44db10a2b9e30346796c2d9e9f44722

 Response:

 Status Code: 200 OK
 Content-Type: application/json
 {
     "token": "f48db3829c71b9dc4957e3bb7b804bd0d44db10a2b9e30346796c2d9e9f44722",
     "refreshToken": "fe45a6ea3d7a0f2cacc864523ab586551739bcbe1a167fe1d37edc6b004ad452",
     "expiresIn": "120",
     "type": "Bearer",
     "scope": "test_scope",
     "valid": true,
     "clientId": "b9db6d84dc98a895035e68f972e30503d3c724c8",
     "codeId": "",
     "userId": "12345",
     "created": 1432542201299,
     "refreshExpiresIn": "300"
 }
 */

public class OAuth2ApifestAuthenticatorVerticle extends AuthenticatorVerticle {

//    private static Logger logger = LogManager.getLogger("mqtt-broker-log");

    @Override
    public void startAuthenticator(String address, AuthenticatorConfig c) throws Exception {

        String identityURL = c.getIdpUrl();
        String idp_userName = c.getIdpUsername();
        String idp_password = c.getIdpPassword();
        String app_key = c.getAppKey();
        String app_secret = c.getAppSecret();

        MessageConsumer<JsonObject> consumer = vertx.eventBus().consumer(address, (Message<JsonObject> msg) -> {
            JsonObject oauth2_token = msg.body();
            String username = oauth2_token.getString("username");
            String password = oauth2_token.getString("password");

            // token validation
            try {
                HttpClientOptions opt = new HttpClientOptions();
                HttpClient httpClient = vertx.createHttpClient(opt);
                URL url = new URL(identityURL);

                String uri = url.getPath() + "/validate?token=" + username;
                HttpClientRequest validateReq = httpClient.get(url.getPort(), url.getHost(), uri, resp -> {
                    resp.exceptionHandler(e -> {
                        logger.fatal(e.getMessage(), e);

                        AuthorizationClient.ValidationInfo vi = new AuthorizationClient.ValidationInfo();
                        vi.auth_valid = false;
                        vi.authorized_user = "";
                        vi.error_msg = e.getMessage();
                        msg.reply(vi.toJson());
                    });
                    resp.bodyHandler(totalBuffer -> {
                        String jsonResponse = totalBuffer.toString("UTF-8");
                        logger.info(jsonResponse);

                        JsonObject j = new JsonObject(jsonResponse);
                        String token = j.getString("token");
                        String refreshToken = j.getString("refreshToken");
                        String expiresIn = j.getString("expiresIn");
                        String type = j.getString("type");
                        String scope = j.getString("scope");
                        boolean valid = j.getBoolean("valid", false);
                        String clientId = j.getString("clientId");
                        String codeId = j.getString("codeId");
                        String userId = j.getString("userId");
                        Long created = j.getLong("created");
                        String refreshExpiresIn = j.getString("refreshExpiresIn");

                        AuthorizationClient.ValidationInfo vi = new AuthorizationClient.ValidationInfo();
                        vi.auth_valid = valid;
                        vi.authorized_user = userId;
                        vi.error_msg = "";

                        JsonObject json = new JsonObject();
                        json = vi.toJson();
                        json.put("scope", scope);
                        json.put("expiry_time", expiresIn);

                        msg.reply(json);
                    });
                });


                HttpClientRequest loginReq = httpClient.post(url.getPort(), url.getHost(), url.getPath(), resp -> {
                    resp.exceptionHandler(e -> {
                        logger.fatal(e.getMessage(), e);

                        AuthorizationClient.ValidationInfo vi = new AuthorizationClient.ValidationInfo();
                        vi.auth_valid = false;
                        vi.authorized_user = "";
                        vi.error_msg = e.getMessage();
                        msg.reply(vi.toJson());
                    });
                    resp.bodyHandler(totalBuffer -> {
                        String jsonResponse = totalBuffer.toString("UTF-8");
                        logger.info(jsonResponse);
                            /*
                            {
                              "access_token": "3a826d62b293f744436617464e893d06b81f95fb39fb971da28bb4d1d1900f61",
                              "refresh_token": "681ab7f865503ca9640739bf4f7a837537daa5cdf11e3a2ff0b3940683598f99",
                              "token_type": "Bearer",
                              "expires_in": "120",
                              "scope": "sp",
                              "refresh_expires_in": "300"
                            }
                             */
                        JsonObject j = new JsonObject(jsonResponse);
                        String new_access_token = j.getString("access_token");
                        String new_refresh_token = j.getString("refresh_token");
                        String token_type = j.getString("token_type");
                        String expires_in = j.getString("expires_in");
                        String scope = j.getString("scope");
                        String refresh_expires_in = j.getString("refresh_expires_in");
                        String error = j.getString("error");

                        AuthorizationClient.ValidationInfo vi = new AuthorizationClient.ValidationInfo();
                        if(new_access_token != null && new_access_token.trim().length()>0) {
                            vi.auth_valid = true;
                            vi.authorized_user = username;
                        } else {
                            vi.auth_valid = false;
                            vi.error_msg = error;
                        }

                        JsonObject json = new JsonObject();
                        json = vi.toJson();
                        json.put("scope", scope);
                        json.put("expiry_time", expires_in);

                        msg.reply(json);
                    });
                });

                if(username.contains("@")) {
                    String data = "grant_type=password&username=" + username + "&password=" + password + "&scope=sp&client_id=" + app_key + "&client_secret=" + app_secret+"";
                    loginReq.putHeader("Content-Type", "application/x-www-form-urlencoded");
//                    loginReq.putHeader("content-length", "");
                    loginReq.end(data, "UTF-8");
                }
                else {
                    validateReq.end();
                }

            } catch (Throwable e) {
                logger.fatal(e.getMessage(), e);

                AuthorizationClient.ValidationInfo vi = new AuthorizationClient.ValidationInfo();
                vi.auth_valid = false;
                vi.authorized_user = "";
                vi.error_msg = e.getMessage();
                msg.reply(vi.toJson());
            }
        });

        logger.info("Startd MQTT Authorization, address: " + consumer.address());
    }

}
