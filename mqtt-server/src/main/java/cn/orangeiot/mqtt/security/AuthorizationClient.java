package cn.orangeiot.mqtt.security;

import java.util.List;

import cn.orangeiot.common.options.SendOptions;
import org.dna.mqtt.moquette.proto.messages.SubscribeMessage.Couple;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * Created by giova_000 on 14/10/2015.
 */
public class AuthorizationClient {

    private static Logger logger = LogManager.getLogger(AuthorizationClient.class);

    private EventBus eventBus;
    private String authenticatorAddress;

    public AuthorizationClient(EventBus eventBus, String authenticatorAddress) {
        this.eventBus = eventBus;
        this.authenticatorAddress = authenticatorAddress;
    }

    public void authorize(String username, String password, String clientID, Handler<ValidationInfo> authHandler) {
        // AUTHENTICATION START
        logger.debug("==AuthorizationClient==authorize==username:" + username + "==password:" + password + "==clientId:" + clientID);
        JsonObject oauth2_token_info = new JsonObject()
                .put("username", username)
                .put("password", password)
                .put("clientId", clientID);

        eventBus.send(authenticatorAddress, oauth2_token_info, (AsyncResult<Message<JsonObject>> res) -> {
            ValidationInfo vi = new ValidationInfo();
            if (res.succeeded()) {
                JsonObject validationInfo = res.result().body();
                logger.debug(validationInfo);
                vi.fromJson(validationInfo);
                if (!res.result().headers().isEmpty())
                    vi.setHeader(res.result().headers().get("status"));
                logger.debug("authenticated ===> " + vi.auth_valid);
                if (vi.auth_valid) {
                    logger.debug("authorized_user ===> " + vi.authorized_user + ", tenant ===> " + vi.tenant);
                    authHandler.handle(vi);
                } else {
                    logger.debug("authenticated error ===> " + vi.error_msg);
                    authHandler.handle(vi);
                }
            } else {
                logger.debug("login failed !");
                vi.auth_valid = Boolean.FALSE;
                authHandler.handle(vi);
            }
        });
    }

    public void authorizePublish(String token, String topic, Handler<Boolean> authHandler) {
        logger.debug("==AuthorizationClient==authorizePublish==token:" + token + "==topic:" + topic);
        JsonObject info = new JsonObject();
        info.put("token", token);
        info.put("topic", topic);
        authHandler.handle(true);
//        eventBus.send(authenticatorAddress + ".publish", info, SendOptions.getInstance(), (AsyncResult<Message<JsonObject>> res) -> {
//            boolean status = false;
//            if (res.succeeded()) {
//                JsonObject reply = res.result().body();
//                if (reply.containsKey("permitted")) {
//                    status = reply.getBoolean("permitted");
//                } else {
//                    status = true;
//                }
//            } else {
//                status = true;
//            }
//
//        });
    }

    public void authorizeSubscribe(String token, List<Couple> topics, Handler<JsonArray> authHandler) {
        logger.debug("==AuthorizationClient==authorizeSubscribe==token:" + token + "==topics:" + topics);
        JsonArray topicArray = new JsonArray();
        topics.forEach(topic -> {
            topicArray.add(topic.getTopicFilter());
        });
        JsonObject info = new JsonObject();
        info.put("token", token);
        info.put("topics", topicArray);
        eventBus.send(authenticatorAddress + ".subscribe", info, (AsyncResult<Message<JsonObject>> res) -> {
            JsonArray status = new JsonArray();
            if (res.succeeded()) {
                JsonObject reply = res.result().body();
                if (reply.containsKey("permitted")) {
                    status = reply.getJsonArray("permitted");
                }
            }
            authHandler.handle(status);
        });
    }

    public static class ValidationInfo {
        public Boolean auth_valid;
        public String authorized_user;
        public String error_msg;
        public String tenant;
        public String token = null;
        public String header = null;

        public String getHeader() {
            return header;
        }

        public void setHeader(String header) {
            this.header = header;
        }

        public void fromJson(JsonObject validationInfo) {
            auth_valid = validationInfo.getBoolean("auth_valid", Boolean.FALSE);
            authorized_user = validationInfo.getString("authorized_user");
            error_msg = validationInfo.getString("error_msg");
            token = validationInfo.getString("token");
            if (auth_valid) {
                tenant = extractTenant(authorized_user);
            }
        }

        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.put("auth_valid", auth_valid);
            json.put("authorized_user", authorized_user);
            json.put("error_msg", error_msg);
            if (token != null) {
                json.put("token", token);
            }
            return json;
        }

        private String extractTenant(String username) {
            if (username == null || username.trim().length() == 0)
                return "";
            String tenant = "";
            int idx = username.lastIndexOf('@');
            if (idx > 0) {
                tenant = username.substring(idx + 1);
            }
            return tenant;
        }
    }


}
