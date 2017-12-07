package cn.orangeiot.mqtt.security.impl;

import cn.orangeiot.mqtt.security.AuthorizationClient;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

/**
 * Created by giova_000 on 15/10/2015.
 */
public class SimpleAuthenticatorVerticle extends AuthenticatorVerticle {
    private JsonObject users;
    @Override
    public void startAuthenticator(String address, AuthenticatorConfig c) throws Exception {

        users = config().getJsonObject("users", new JsonObject());

        vertx.eventBus().consumer(address, (Message<JsonObject> msg) -> {
            JsonObject oauth2_token = msg.body();
            String username = oauth2_token.getString("username");
            String password = oauth2_token.getString("password");

            // token validation
            JsonObject json = new JsonObject();
            Boolean tokanIsValid = Boolean.FALSE;
            try {
                if(users.containsKey(username)) {
                    String pw = users.getString(username);
                    if(pw.equals(password)) {
                        tokanIsValid = Boolean.TRUE;
                    }
                }
                AuthorizationClient.ValidationInfo vi = new AuthorizationClient.ValidationInfo();
                vi.auth_valid = tokanIsValid;
                vi.authorized_user = username;
                vi.error_msg = "";
                json = vi.toJson();
            } catch (Exception e) {
                e.printStackTrace();
            }
            msg.reply(json);
        });
    }

}
