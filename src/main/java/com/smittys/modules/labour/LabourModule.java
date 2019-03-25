package com.smittys.modules.labour;

import com.smittys.Tracker;
import com.smittys.entities.WebSection;
import com.smittys.modules.WebModule;
import spark.Request;
import spark.Response;

import java.util.HashMap;
import java.util.Properties;

public class LabourModule extends WebModule {

    private HashMap<String, WebSection> sections;

    public LabourModule() {
        sections = new HashMap<>();
        loadSections();
    }

    public void loadSections() {
        sections.put("hours", new HoursSection());
        sections.put("employees", new EmployeesSection());
        sections.put("sales", new SalesSection());
        sections.put("tips", new TipsSection());
    }

    @Override
    public String[] getEndpoints() {
        return new String[]{"GET", "/labour", "GET", "/labour/:section", "GET", "/labour/:section/:action", "POST", "/labour/:section/:action"};
    }

    @Override
    public Object decodeRequest(String endpoint, Request request, Response response) {
        Properties prop = new Properties();
        HashMap<String, Object> model = new HashMap<>();
        String method = request.requestMethod();
        if (!isLoggedIn(request)) return WebModule.redirect("/?redirect=" + request.pathInfo(), 0, request, response);
        try {
            switch (endpoint) {
                case "/labour":
                case "/labour/:section":
                    String section = request.params(":section");
                    if (section == null) section = "hours";
                    if (method.equals("GET")) {
                        model.put("section", section + (request.queryString() != null ? "?" + request.queryString() : ""));
                        return render("./source/modules/labour/index.jade", model, request, response);
                    }
                    break;
                case "/labour/:section/:action":
                    section = request.params(":section");
                    if (!sections.containsKey(section)) return error("Section not found.");
                    String action = request.params(":action");
                    if (action == null) return error("No action specified.");
                    return sections.get(section).decode(action, request, response);
            }
        } catch (Exception e) {
            e.printStackTrace();
            prop.put("success", false);
            prop.put("error", e.getMessage());
        }
        return Tracker.getGson().toJson(prop);
    }
}
