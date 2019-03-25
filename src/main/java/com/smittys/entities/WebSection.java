package com.smittys.entities;

import spark.Request;
import spark.Response;

public interface WebSection {

    String getName();

    String decode(String action, Request request, Response response);

}
