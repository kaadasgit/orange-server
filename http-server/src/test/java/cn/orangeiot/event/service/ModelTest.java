package cn.orangeiot.event.service;

import io.vertx.ext.web.RoutingContext;

import java.io.Serializable;
import java.util.*;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-07-17
 */
public class ModelTest implements Serializable {

    private Set<String> urlSet = new HashSet<>();

    private Set<String> methodSet = new HashSet<>();

    private List<BaseHandler<RoutingContext>> listsHandler = new ArrayList<>();


    public Set<String> getUrl() {
        return urlSet;
    }

    public ModelTest setUrl(String url) {
        urlSet.add(url);
        return this;
    }

    public Set<String> getMethod() {
        return methodSet;
    }

    public ModelTest setMethod(String method) {
        methodSet.add(method);
        return this;
    }

    public List<BaseHandler<RoutingContext>> getListsHandler() {
        return listsHandler;
    }

    public ModelTest addHandler(BaseHandler<RoutingContext> handler) {
        listsHandler.add(handler);
        return this;
    }
}
