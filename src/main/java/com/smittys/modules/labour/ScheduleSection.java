package com.smittys.modules.labour;

import com.smittys.Tracker;
import com.smittys.entities.WebSection;
import com.smittys.modules.WebModule;
import spark.Request;
import spark.Response;

import java.util.HashMap;
import java.util.Properties;

public class ScheduleSection implements WebSection {

    @Override
    public String getName() {
        return "schedules";
    }

    @Override
    public String decode(String action, Request request, Response response) {
        Properties prop = new Properties();
        HashMap<String, Object> model = new HashMap<>();
        switch (action) {
            case "load":
                String html;
                try {
                    html = WebModule.render("./source/modules/labour/schedules/schedule.jade", model, request, response);
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
            case "load-list":
                break;
            case "add-schedule":
                break;
        }
        return Tracker.getGson().toJson(prop);
    }
}
