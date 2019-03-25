package com.smittys.modules.inventory;

import com.smittys.Tracker;
import com.smittys.entities.WebSection;
import com.smittys.modules.WebModule;
import spark.Request;
import spark.Response;

import java.util.HashMap;
import java.util.Properties;

public class InventoryModule extends WebModule {

    private HashMap<String, WebSection> sections;

    public InventoryModule() {
        sections = new HashMap<>();
        loadSections();
    }

    public void loadSections() {
        sections.put("recipe-items", new RecipeItemModule());
        sections.put("invoices", new InvoicesModule());
        sections.put("invoice-items", new InvoiceItemsModule());
        sections.put("meals", new KitchenMealsSection());
    }

    @Override
    public String[] getEndpoints() {
        return new String[]{"GET", "/inventory", "GET", "/inventory/:section", "POST", "/inventory/:section/:action", "GET", "/inventory/:section/:action", "POST", "/inventory/:section"};
    }

    @Override
    public Object decodeRequest(String endpoint, Request request, Response response) {
        Properties prop = new Properties();
        HashMap<String, Object> model = new HashMap<>();
        String method = request.requestMethod();
        if (!isLoggedIn(request)) return WebModule.redirect("/", 0, request, response);
        switch (endpoint) {
            case "/inventory":
            case "/inventory/:section":
                String section = request.params(":section");
                if (section == null) section = "invoices";
                if (method.equals("GET")) {
                    model.put("section", section);
                    return render("./source/modules/inventory/index.jade", model, request, response);
                }
                break;
            case "/inventory/:section/:action":
                String action = request.params(":action");
                section = request.params(":section");
                if (!sections.containsKey(section)) return error("Section not found.");
                return sections.get(section).decode(action, request, response);
        }
        return Tracker.getGson().toJson(prop);
    }
}
