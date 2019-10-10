package com.cryo.modules.labour;

import com.cryo.Tracker;
import com.cryo.entities.WebSection;
import com.cryo.modules.WebModule;
import spark.Request;
import spark.Response;

import java.util.HashMap;
import java.util.Properties;

public class TipsSection implements WebSection {
    @Override
    public String getName() {
        return "tips";
    }

    @Override
    public String decode(String action, Request request, Response response) {
        HashMap<String, Object> model = new HashMap<>();
        Properties prop = new Properties();
        String method = request.requestMethod();
        switch (action) {
            case "load":
                String html;
                try {
                    html = WebModule.render("./source/modules/labour/tips/tips.jade", model, request, response);
                } catch (Exception e) {
                    e.printStackTrace();
                    prop.put("success", false);
                    prop.put("error", e.getMessage());
                    break;
                }
                if (html == null) {
                    prop.put("success", false);
                    prop.put("error", "Unable to load section.");
                    break;
                }
                prop.put("success", true);
                prop.put("html", html);
                break;
            default:
                prop.put("success", false);
                prop.put("error", "Invalid action.");
                break;
        }
        return Tracker.getGson().toJson(prop);
    }
}
