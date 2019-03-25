package com.smittys.modules.login;

import com.smittys.Tracker;
import com.smittys.db.impl.UserConnection;
import com.smittys.modules.WebModule;
import spark.Request;
import spark.Response;

import java.util.Properties;

public class LoginModule extends WebModule {

    @Override
    public String[] getEndpoints() {
        return new String[]{"POST", "/login"};
    }

    @Override
    public Object decodeRequest(String endpoint, Request request, Response response) {
        String path = request.queryParams("redirect");
        if (path == null) path = "/";
        if (isLoggedIn(request))
            return redirect(path, request, response);
        String username = request.queryParams("username");
        String password = request.queryParams("password");
        Object[] data = UserConnection.connection().handleRequest("compare", username, password);
        if (data == null || !((boolean) data[0])) return error("Invalid username or password.");
        data = UserConnection.connection().handleRequest("add-sess", username);
        if (data == null) return error("Error adding new session.");
        String sessionId = (String) data[0];
        response.cookie("smittys-sess", sessionId, (30 * 24 * 60 * 60));
        String redirect = redirect(path, request, response);
        Properties prop = new Properties();
        prop.put("success", true);
        prop.put("html", redirect);
        return Tracker.getGson().toJson(prop);
    }
}
