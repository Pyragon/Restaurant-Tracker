package com.smittys.modules.inventory;

import com.smittys.Tracker;
import com.smittys.entities.WebSection;
import com.smittys.modules.WebModule;
import spark.Request;
import spark.Response;

import java.util.HashMap;
import java.util.Properties;

public class ModifiersSection implements WebSection {

    @Override
    public String getName() {
        return "modifiers";
    }

    @Override
    public String decode(String action, Request request, Response response) {
        HashMap<String, Object> model = new HashMap<>();
        Properties prop = new Properties();
        switch (action) {
            case "load":
                String html;
                try {
                    html = WebModule.render("./source/modules/inventory/modifiers/modifiers.jade", model, request, response);
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
        }
        return Tracker.getGson().toJson(prop);
    }
}
