package com.smittys.modules;

import spark.Request;
import spark.Response;

import java.util.HashMap;

public class TestModule extends WebModule {

    @Override
    public String[] getEndpoints() {
        return new String[] {
                "GET", "/test",
                "GET", "/testdef"
        };
    }

    @Override
    public Object decodeRequest(String endpoint, Request request, Response response) {
        switch(endpoint) {
            case "/test":
                return render("./source/modules/test/test.jade", new HashMap<>(), request, response);
            case "/testdef":
                return render("./source/modules/test/testdef.jade", new HashMap<>(), request, response);
        }
        return null;
    }
}
