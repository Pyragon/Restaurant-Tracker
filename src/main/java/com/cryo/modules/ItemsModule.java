package com.cryo.modules;

import spark.Request;
import spark.Response;

public class ItemsModule extends WebModule {
    @Override
    public String[] getEndpoints() {
        return new String[]{
                "GET", "/items",
                "GET", "/items/add",
                "POST", "/items/add",
                "GET", "/items/recipes",
                "GET", "/items/recipes/add",
                "POST", "/items/recipes/add"
        };
    }

    @Override
    public Object decodeRequest(String endpoint, Request request, Response response) {
        return null;
    }
}
