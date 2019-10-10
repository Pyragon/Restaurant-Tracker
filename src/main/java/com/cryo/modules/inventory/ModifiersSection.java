package com.cryo.modules.inventory;

import com.cryo.Tracker;
import com.cryo.db.impl.InventoryConnection;
import com.cryo.entities.Modifier;
import com.cryo.entities.WebSection;
import com.cryo.modules.WebModule;
import spark.Request;
import spark.Response;

import java.util.ArrayList;
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
            case "load-list":
                int page = Integer.parseInt(request.queryParams("page"));
                ArrayList<Modifier> modifiers = InventoryConnection.connection().selectListPage("modifiers", page, Modifier.class);
                if (modifiers == null) {
                    prop.put("success", false);
                    prop.put("error", "Error loading modifier list.");
                    break;
                }
                model.put("modifiers", modifiers);
                int count = InventoryConnection.connection().selectCountRounded("modifiers");
                prop.put("success", true);
                prop.put("html", WebModule.render("./source/modules/inventory/modifiers/modifiers_list.jade", model, request, response));
                prop.put("pageTotal", count);
                break;
            case "add-edit":
                if(request.requestMethod().equals("GET")) {
                    String idString = request.queryParams("id");
                    if(idString == null) {
                        prop.put("success", true);
                        prop.put("html", WebModule.render("./source/modules/inventory/modifiers/add_edit_modifier.jade", new HashMap<>(), request, response));
                        break;
                    }
                }
                prop.put("success", false);
                prop.put("error", "Error loading.");
                break;
        }
        return Tracker.getGson().toJson(prop);
    }
}
