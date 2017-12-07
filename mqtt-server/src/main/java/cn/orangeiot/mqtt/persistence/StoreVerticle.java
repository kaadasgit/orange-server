package cn.orangeiot.mqtt.persistence;

import cn.orangeiot.mqtt.MQTTTopicsManagerOptimized;
import cn.orangeiot.mqtt.ITopicsManager;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.MultiMap;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.commons.collections4.map.PassiveExpiringMap;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Created by Giovanni Baleani on 04/06/2015.
 */
public class StoreVerticle extends AbstractVerticle {

    public static final String ADDRESS = StoreVerticle.class.getName()+"_IN";

    private Map<String, Map<String, byte[]>> db;
    private ITopicsManager topicsManager;

    @Override
    public void start() throws Exception {
        PassiveExpiringMap.ConstantTimeToLiveExpirationPolicy<String, Map<String, byte[]>>
                expirePeriod = new PassiveExpiringMap.ConstantTimeToLiveExpirationPolicy<>(
                        1, TimeUnit.DAYS);
        this.db = new PassiveExpiringMap<>( expirePeriod, new LinkedHashMap<>() );
        this.topicsManager = new MQTTTopicsManagerOptimized();

        MessageConsumer<JsonObject> consumer = vertx.eventBus().consumer(ADDRESS);
        consumer.handler(message -> {
            JsonObject request = message.body();
            MultiMap headers = message.headers();
            if (headers == null || !headers.contains("command")) {
                message.reply(new JsonObject().put("error", "Invalid message: missing 'command' header"));
            }
            JsonObject response = new JsonObject();
            String command = headers.get("command");
            switch (command) {
                case "saveRetainMessage":
                    response = saveRetainMessage(request);
                    break;
                case "getRetainedMessagesByTopicFilter":
                    response = getRetainedMessagesByTopicFilter(request);
                    break;
                case "deleteRetainMessage":
                    response = deleteRetainMessage(request);
                    break;
                default:
                    response = doDefault(request);
                    break;
            }
//            System.out.println("instance => "+ this + "db.size => "+ db.size());
            message.reply(response);
        });

    }

    private Map<String, byte[]> db(String tenant) {
        if(!db.containsKey(tenant)) {
            db.put(tenant, new LinkedHashMap<>());
        }
        return db.get(tenant);
    }
    private Set<String> dbTenants() {
        if(db != null)
            return db.keySet();
        return Collections.emptySet();
    }



    private JsonObject saveRetainMessage(JsonObject request) {
        String topic = request.getString("topic");
        String tenant = request.getString("tenant");
        byte[] message = request.getBinary("message");
        Map<String, byte[]> db = db(tenant);
        db.put(topic, message);

        JsonObject response = new JsonObject();
        response.put("topic", topic).put("message", message);
        return response;
    }

    private JsonObject getRetainedMessagesByTopicFilter(JsonObject request) {
        String topicFilter = request.getString("topicFilter");
        String tenant = request.getString("tenant");
        List<JsonObject> list = new ArrayList<>();
        if(tenant != null && tenant.trim().length()>0) {
            List<JsonObject> alist = getRetainedMessagesByTopicFilter(tenant, topicFilter);
            list.addAll(alist);
        } else {
            Set<String> tenants = dbTenants();
            for (String atenant : tenants) {
                List<JsonObject> alist = getRetainedMessagesByTopicFilter(atenant, topicFilter);
                list.addAll(alist);
            }
        }

        JsonObject response = new JsonObject();
        response.put("results", new JsonArray(list));
        return response;
    }

    private List<JsonObject> getRetainedMessagesByTopicFilter(String tenant, String topicFilter) {
        List<JsonObject> list = new ArrayList<>();
        if(tenant != null) {
            Map<String, byte[]> db = db(tenant);

            for (String topic : db.keySet()) {
                boolean topicMatch = topicsManager.match(topic, topicFilter);
                if (topicMatch) {
                    byte[] message = db.get(topic);
                    JsonObject item = new JsonObject().put("topic", topic).put("message", message);
                    list.add(item);
                }
            }
        }
        return list;
    }

    private JsonObject deleteRetainMessage(JsonObject request) {
        String topic = request.getString("topic");
        String tenant = request.getString("tenant");
        Map<String, byte[]> db = db(tenant);
        db.remove(topic);

        JsonObject response = new JsonObject();
        response.put("success", true);
        return response;
    }

    private JsonObject doDefault(JsonObject request) {
        JsonObject response = new JsonObject();
        return response;
    }
}
