package com.smittys.modules.inventory;

import com.smittys.Tracker;
import com.smittys.db.impl.InventoryConnection;
import com.smittys.entities.ItemData;
import com.smittys.entities.RecipeItem;
import com.smittys.entities.WebSection;
import com.smittys.modules.WebModule;
import spark.Request;
import spark.Response;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

public class InvoiceItemsModule implements WebSection {

    @Override
    public String getName() {
        return "invoice-items";
    }

    @Override
    public String decode(String action, Request request, Response response) {
        HashMap<String, Object> model = new HashMap<>();
        Properties prop = new Properties();
        switch (action) {
            case "load":
                String html;
                try {
                    html = WebModule.render("./source/modules/inventory/items/invoice/invoice_items.jade", model, request, response);
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
                Object[] data = InventoryConnection.connection().handleRequest("get-items", page);
                if (data == null) {
                    prop.put("success", false);
                    prop.put("error", "Error loading recipe items.");
                    break;
                }
                ArrayList<RecipeItem> items = (ArrayList<RecipeItem>) data[0];
                model.put("items", items);
                data = InventoryConnection.connection().handleRequest("get-items-count");
                if (data == null) {
                    prop.put("success", false);
                    prop.put("error", "Error getting page total.");
                    break;
                }
                prop.put("success", true);
                prop.put("html", WebModule.render("./source/modules/inventory/items/invoice/invoice_items_list.jade", model, request, response));
                prop.put("pageTotal", data[0]);
                break;
            case "add-unit":
                if (request.requestMethod().equalsIgnoreCase("GET")) {
                    int id = Integer.parseInt(request.queryParams("id"));
                    data = InventoryConnection.connection().handleRequest("get-item-by-id", id);
                    if (data == null) {
                        prop.put("success", false);
                        prop.put("error", "Unable to find item.");
                        break;
                    }
                    ItemData item = (ItemData) data[0];
                    html = WebModule.render("./source/modules/inventory/items/invoice/missing_unit.jade", model, request, response);
                    prop.put("success", true);
                    prop.put("html", html);
                    prop.put("name", item.getDisplayName());
                    break;
                } else
                    return InvoiceItemsModule.addUnit(Integer.parseInt(request.queryParams("id")), request.queryParams("unit"), request.queryParams("quantity"));
        }
        return Tracker.getGson().toJson(prop);
    }

    public static String addUnit(int id, String unit, String value) {
        Properties prop = new Properties();
        while (true) {
            double quantity;
            try {
                quantity = Double.parseDouble(value);
            } catch (Exception e) {
                prop.put("success", false);
                prop.put("error", "Quantity must be a double.");
                break;
            }
            Object[] itemD = InventoryConnection.connection().handleRequest("get-item-by-id", id);
            if (itemD == null) {
                prop.put("success", false);
                prop.put("error", "Error loading item data.");
                break;
            }
            ItemData item = (ItemData) itemD[0];
            Properties units = item.getUnitProps();
            units.put(unit, quantity);
            InventoryConnection.connection().handleRequest("update-units", id, Tracker.getGson().toJson(units));
            prop.put("success", true);
            prop.put("message", "Successfully updated units.");
            break;
        }
        return Tracker.getGson().toJson(prop);
    }
}
