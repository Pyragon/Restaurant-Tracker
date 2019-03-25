package com.smittys.modules.login;

import com.smittys.modules.WebModule;
import spark.Request;
import spark.Response;

public class LogoutModule extends WebModule {

    @Override
    public String[] getEndpoints() {
        return new String[]{"POST", "/logout", "GET", "/logout"};
    }

    @Override
    public Object decodeRequest(String endpoint, Request request, Response response) {
        if (isLoggedIn(request))
            response.removeCookie("smittys-sess");
        return redirect("/", request, response);
    }
}
