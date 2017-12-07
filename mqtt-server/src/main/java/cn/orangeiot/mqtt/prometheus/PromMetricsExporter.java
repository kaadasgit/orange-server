package cn.orangeiot.mqtt.prometheus;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.hotspot.DefaultExports;
import io.prometheus.client.vertx.MetricsHandler;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;

/**
 * Created by giovanni on 07/07/17.
 */
public class PromMetricsExporter extends AbstractVerticle {
    @Override
    public void start() throws Exception {
        JsonObject conf = config().getJsonObject("prometheus_exporter", new JsonObject());
        int httpPort = conf.getInteger("port", 9100);
        String path = conf.getString("path", "/metrics");

        DefaultExports.initialize();

        Router router = Router.router(vertx);
        router.route(path).handler(new MetricsHandler());
        vertx.createHttpServer().requestHandler(router::accept).listen(httpPort);
    }
}
