package com.cryo.managers;

import com.cryo.Tracker;
import com.cryo.db.impl.ErrorConnection;
import com.cryo.entities.EnterHoursError;
import com.cryo.entities.Error;
import com.cryo.entities.UnitError;
import com.cryo.modules.WebModule;
import spark.Request;
import spark.Response;
import spark.Spark;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.cryo.modules.WebModule.error;

public class ErrorManager {

    private HashMap<String, Error> errors;

    public ErrorManager() {
        errors = new HashMap<>();
        load();
    }

    public static String[] ROUTES = {"GET", "/errors/reload/:type", "POST", "/errors/rebuild/:type", "POST", "/errors/reload/:type", "POST", "/errors/recheck/:key", "GET", "/errors/click", "POST", "/errors/click", "GET", "/errors/button", "POST", "/errors/button"};

    private void load() {
        Object[] data = ErrorConnection.connection().handleRequest("get-errors", true);
        if (data == null) return;
        ArrayList<Error> errors = (ArrayList<Error>) data[0];
        errors.stream().map(this::buildError).forEach(e -> this.errors.put(e.getKey(), e));
    }

    public Error buildError(Error error) {
        String key = error.getKey().toLowerCase();
        if (key.contains("missing-unit"))
            return new UnitError(error.getKey(), error.getShortMessage(), error.getParameters().getProperty("unit"), Integer.parseInt(error.getParameters().getProperty("id")));
        else if (key.contains("missing-hours"))
            return new EnterHoursError(error.getKey(), error.getShortMessage());
        return error;
    }

    public void registerEndpoints() {
        int index = 0;
        while (index < ROUTES.length) {
            String requestType = ROUTES[index++];
            String route = ROUTES[index++];
            if (requestType.equals("GET")) Spark.get(route, (req, res) -> handleRequest(route, req, res));
            else if (requestType.equals("POST")) Spark.post(route, (req, res) -> handleRequest(route, req, res));
        }
    }

    public String handleRequest(String endpoint, Request request, Response response) {
        boolean loggedIn = WebModule.isLoggedIn(request);
        if (!loggedIn) return WebModule.redirect("/", 0, request, response);
        Properties prop = new Properties();
        switch (endpoint) {
            case "/errors/recheck/:key":
                String key = request.params(":key");
                recheck(key);
                prop.put("success", true);
                break;
            case "/errors/reload/:type":
                int type = Integer.parseInt(request.params(":type"));
                Error.ErrorParent parent = Error.ErrorParent.getError(type);
                if (parent == null) return error("Error loading error type.");
                prop.put("success", true);
                ArrayList<Properties> retList = new ArrayList<>();
                List<Error> errorList = getErrors(parent, 5);
                for (Error error : errorList) {
                    Properties errorProp = new Properties();
                    errorProp.put("key", error.getKey());
                    errorProp.put("shortMessage", error.getShortMessage());
                    errorProp.put("type", error.getType());
                    errorProp.put("hasLeftClick", error.hasLeftClick());
                    errorProp.put("refreshAfterClick", error.refreshAfterClick());
                    errorProp.put("opensModal", error.opensModal());
                    if (error.getLink() != null) errorProp.put("link", error.getLink());
                    retList.add(errorProp);
                }
                prop.put("errors", retList);
                break;
            case "/errors/rebuild/:type":
                type = Integer.parseInt(request.params(":type"));
                parent = Error.ErrorParent.getError(type);
                if (parent == null) return error("Error loading error type.");
                rebuild(parent);
                prop.put("success", true);
                prop.put("name", parent.name());
                break;
            case "/errors/button":
                try {
                    // System.out.println(request.queryParams("data"));
                    Properties properties = Tracker.getGson().fromJson(request.queryParams("data"), Properties.class);
                    key = properties.getProperty("key");
                    if (!errors.containsKey(key)) return error("Cannot find error.");
                    Error error = errors.get(key);
                    if (!error.opensModal()) return error("This error shouldn't have a modal");
                    String button = properties.getProperty("button");
                    if (button == null) return error("Invalid button provided.");
                    return error.buttonClick(button, properties);
                } catch (Exception e) {
                    e.printStackTrace();
                    return error("Unable to process button click. Please close and retry.");
                }
            case "/errors/click":
                key = request.queryParams("key");
                if (!errors.containsKey(key)) return error("Cannot find error.");
                Error error = errors.get(key);
                if (!error.hasLeftClick()) return error("Error has no left click option.");
                if (!error.opensModal()) return error.click();
                Properties modalData = error.getModalData(request, response);
                if (modalData == null) return error("Error loading modal data for error.");
                prop = modalData;
                break;
            default:
                return WebModule.render404(request, response);
        }
        return Tracker.getGson().toJson(prop);
    }

    public HashMap<String, Object> parseButtons(Request request, Response response) {
        HashMap<String, Object> model = new HashMap<>();
        return model;
    }

    public void rebuild(Error.ErrorParent parent) {
        getErrors(parent, -1).forEach(e -> recheck(e.getKey()));
    }

    public void recheck(String key) {
        if (key != null) {
            if (errors.containsKey(key)) {
                if (!errors.get(key).recheck()) {
                    ErrorConnection.connection().handleRequest("set-solved", errors.remove(key));
                    return;
                }
            }
        }
    }

    public void add(Error error) {
        if (errors.containsKey(error.getKey())) return;
        errors.put(error.getKey(), error);
        ErrorConnection.connection().handleRequest("add-error", error);
    }

    public List<Error> getErrors(Error.ErrorParent parent, int limit) {
        Stream<Error> stream = errors.values().stream().filter(e -> e.getParent() == parent);
//        if (parent == Error.ErrorParent.INVENTORY) stream = stream.sorted(Comparator.comparingInt(Error::getId));
        if (parent == Error.ErrorParent.INVENTORY) stream = stream.filter(s -> s instanceof UnitError);
        if (limit != -1) stream = stream.limit(limit);
        return stream.collect(Collectors.toList());
    }

    public Error getError(String name) {
        return errors.get(name);
    }
}
