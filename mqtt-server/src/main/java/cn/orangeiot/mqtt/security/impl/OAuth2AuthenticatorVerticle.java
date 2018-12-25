package cn.orangeiot.mqtt.security.impl;

import cn.orangeiot.mqtt.security.AuthorizationClient;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.json.JsonObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Base64;

/**
 * Created by Giovanni Baleani on 04/02/2015.
 */

public class OAuth2AuthenticatorVerticle extends AuthenticatorVerticle {

    private Oauth2TokenValidator oauth2Validator;

    @Override
    public void startAuthenticator(String address, AuthenticatorConfig c) throws Exception {

        String identityURL = c.getIdpUrl();
        String idp_userName = c.getIdpUsername();
        String idp_password = c.getIdpPassword();
        String app_key = c.getAppKey();
        String app_secret = c.getAppSecret();

        oauth2Validator = new Oauth2TokenValidator(identityURL, idp_userName, idp_password);

        MessageConsumer<JsonObject> consumer = vertx.eventBus().consumer(address, (Message<JsonObject> msg) -> {
            JsonObject oauth2_token = msg.body();
            String usernameOrAccessToken = oauth2_token.getString("username");
            String passwordOrRefreshToken = oauth2_token.getString("password");
            // token validation
            JsonObject json = new JsonObject();
            Boolean tokanIsValid = Boolean.FALSE;
            try {
                if(usernameOrAccessToken.contains("@")) {
                    Future<AuthorizationClient.ValidationInfo> f = Future.future();
                    f.setHandler(event -> {
                        AuthorizationClient.ValidationInfo vi = event.result();
                        msg.reply(vi.toJson());
                    });
                    performLoginRequest(f, identityURL, usernameOrAccessToken, passwordOrRefreshToken, app_key, app_secret);
                } else {
                    tokanIsValid = oauth2Validator.tokenIsValid(usernameOrAccessToken);
                    TokenInfo info = oauth2Validator.getTokenInfo(usernameOrAccessToken);
                    AuthorizationClient.ValidationInfo vi = new AuthorizationClient.ValidationInfo();
                    vi.auth_valid = tokanIsValid;
                    vi.authorized_user = info.getAuthorizedUser();
                    vi.error_msg = info.getErrorMsg();

                    json = vi.toJson();
                    json.put("scope", info.getScope());
                    json.put("expiry_time", info.getExpiryTime());
                    msg.reply(json);
                }
            } catch (Exception e) {
                logger.fatal(e.getMessage(), e);
            }
        });

        logger.info("Startd MQTT Authorization, address: " + consumer.address());
    }


    private void performLoginRequest(Future<AuthorizationClient.ValidationInfo> f
            , String identityURL
            , String username
            , String password
            , String app_key
            , String app_secret
    ) throws MalformedURLException {
        HttpClientOptions opt = new HttpClientOptions();
        HttpClient httpClient = vertx.createHttpClient(opt);
        URL url = new URL(identityURL + "/oauth2/token");
        logger.info("auth url "+ url);
        HttpClientRequest loginReq = httpClient.post(url.getPort(), url.getHost(), url.getPath(), resp -> {
            resp.exceptionHandler(e -> {
                logger.fatal(e.getMessage(), e);

                AuthorizationClient.ValidationInfo vi = new AuthorizationClient.ValidationInfo();
                vi.auth_valid = false;
                vi.authorized_user = "";
                vi.error_msg = e.getMessage();
//                msg.reply(vi.toJson());
                f.complete(vi);
            });
            resp.bodyHandler(totalBuffer -> {
                String jsonResponse = totalBuffer.toString("UTF-8");
                logger.info(jsonResponse);
                            /*
                            { APIFEST
                              "access_token": "3a826d62b293f744436617464e893d06b81f95fb39fb971da28bb4d1d1900f61",
                              "refresh_token": "681ab7f865503ca9640739bf4f7a837537daa5cdf11e3a2ff0b3940683598f99",
                              "token_type": "Bearer",
                              "expires_in": "120",
                              "scope": "sp",
                              "refresh_expires_in": "300"
                            }
                            { WSO2
                              "access_token":"454268acb7eedea79276cb4679c2d6"
                              "refresh_token":"205a17f38d84645a664bee716ee27e6",
                              "token_type":"bearer",
                              "expires_in":2560,
                            }
                            */
                JsonObject j = new JsonObject(jsonResponse);
                String new_access_token = j.getString("access_token");
                String new_refresh_token = j.getString("refresh_token");
                String token_type = j.getString("token_type");
                Integer expires_in = j.getInteger("expires_in");
                String scope = j.getString("scope");
                Integer refresh_expires_in = j.getInteger("refresh_expires_in");
                String error = j.getString("error");

                AuthorizationClient.ValidationInfo vi = new AuthorizationClient.ValidationInfo();
                if(new_access_token != null && new_access_token.trim().length()>0) {
                    vi.auth_valid = true;
                    vi.authorized_user = username;
                } else {
                    vi.auth_valid = false;
                    vi.error_msg = error;
                }

//                JsonObject json = new JsonObject();
//                json = vi.toJson();
//                json.put("scope", scope);
//                json.put("expiry_time", expires_in);

//                msg.reply(json);
                f.complete(vi);
            });
        });
        String ks = app_key+":"+app_secret;
        String base64keysecret = Base64.getEncoder().encodeToString(ks.getBytes());
        loginReq.putHeader("Content-Type", "application/x-www-form-urlencoded");
        loginReq.putHeader("Authorization", "Basic "+base64keysecret);
        String data = "grant_type=password&username=" + username + "&password=" + password + "";
        loginReq.end(data, "UTF-8");
    }
}
