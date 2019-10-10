package com.cryo.modules.inventory;

import com.cryo.Tracker;
import com.cryo.db.impl.InventoryConnection;
import com.cryo.db.impl.LabourConnection;
import com.cryo.entities.Employee;
import com.cryo.entities.KitchenMeal;
import com.cryo.entities.RecipeItem;
import com.cryo.entities.WebSection;
import com.cryo.modules.WebModule;
import com.cryo.utils.Utilities;
import org.apache.commons.lang3.math.NumberUtils;
import spark.Request;
import spark.Response;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

public class KitchenMealsSection implements WebSection {

    @Override
    public String getName() {
        return "meals";
    }

    @Override
    public String decode(String action, Request request, Response response) {
        HashMap<String, Object> model = new HashMap<>();
        Properties prop = new Properties();
        switch (action) {
            case "load":
                String html;
                try {
                    html = WebModule.render("./source/modules/inventory/meals/meals.jade", model, request, response);
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
                Object[] data = InventoryConnection.connection().handleRequest("get-kitchen-meals", page);
                if (data == null) {
                    prop.put("success", false);
                    prop.put("error", "Error loading kitchen meals.");
                    break;
                }
                ArrayList<KitchenMeal> meals = (ArrayList<KitchenMeal>) data[0];
                model.put("meals", meals);
                data = InventoryConnection.connection().handleRequest("get-kitchen-meals-count");
                if (data == null) {
                    prop.put("success", false);
                    prop.put("error", "Error getting page total.");
                    break;
                }
                prop.put("success", true);
                prop.put("html", WebModule.render("./source/modules/inventory/meals/meals_list.jade", model, request, response));
                prop.put("pageTotal", data[0]);
                break;
            case "add":
                if (request.requestMethod().equals("GET")) {
                    String idString = request.queryParams("id");
                    if (idString != null) {
                        int id = Integer.parseInt(idString);
                        data = InventoryConnection.connection().handleRequest("get-kitchen-meal", id);
                        if (data != null) {
                            KitchenMeal meal = (KitchenMeal) data[0];
                            //TODO
                        }
                    }
                    html = WebModule.render("./source/modules/inventory/meals/add_kitchen_meal.jade", model, request, response);
                    prop.put("success", true);
                    prop.put("html", html);
                    break;
                } else {
                    String name = request.queryParams("name");
                    String main = request.queryParams("mains");
                    String side = request.queryParams("sides");
                    String mainQuantityString = request.queryParams("mains_quantity");
                    String sideQuantityString = request.queryParams("sides_quantity");
                    if (Utilities.isNullOrEmpty(name)) {
                        prop.put("success", false);
                        prop.put("error", "Name must be filled out!");
                        break;
                    }
                    if (Utilities.isNullOrEmpty(main) && Utilities.isNullOrEmpty(side)) {
                        prop.put("success", false);
                        prop.put("error", "At least 1 main meal or 1 side must be filled out!");
                        break;
                    }
                    data = LabourConnection.connection().handleRequest("get-employee-by-name", name);
                    if (data == null) {
                        prop.put("success", false);
                        prop.put("error", "Cannot find an employee with that name!");
                        break;
                    }
                    Employee employee = (Employee) data[0];
                    if (NumberUtils.isNumber(main))
                        data = InventoryConnection.connection().handleRequest("get-recipe-item-by-id", Integer.parseInt(main));
                    else data = InventoryConnection.connection().handleRequest("get-recipe-item-by-name", main);
                    if (data == null) {
                        prop.put("success", false);
                        prop.put("error", "Unable to find that recipe item!");
                        break;
                    }
                    RecipeItem item = (RecipeItem) data[0];
                    Properties mainMeal = new Properties();
                    Properties quant = new Properties();
                    if (!Utilities.isNullOrEmpty(mainQuantityString)) {
                        double quantity = Double.parseDouble(mainQuantityString.substring(0, mainQuantityString.indexOf(" ")));
                        String unit = mainQuantityString.replace(quantity + " ", "");
                        quant.put("quantity", quantity);
                        quant.put("unit", unit);
                    } else {
                        quant.put("quantity", 1);
                        quant.put("unit", "ea");
                    }
                    //TODO - Create another object, put quant + modifiers into it, pass that to mainMeal
                    mainMeal.put(item.getId(), quant);
                    if (NumberUtils.isNumber(side))
                        data = InventoryConnection.connection().handleRequest("get-recipe-item-by-id", Integer.parseInt(side));
                    else data = InventoryConnection.connection().handleRequest("get-recipe-item-by-name", side);
                    if (data == null) {
                        prop.put("success", false);
                        prop.put("error", "Unable to find that recipe item!");
                        break;
                    }
                    item = (RecipeItem) data[0];
                    Properties sideMeal = new Properties();
                    quant = new Properties();
                    if (!Utilities.isNullOrEmpty(sideQuantityString)) {
                        double quantity = Double.parseDouble(sideQuantityString.substring(0, sideQuantityString.indexOf(" ")));
                        String unit = sideQuantityString.replace(quantity + " ", "");
                        quant.put("quantity", quantity);
                        quant.put("unit", unit);
                    } else {
                        quant.put("quantity", 1);
                        quant.put("unit", "ea");
                    }
                    //TODO - Create another object, put quant + modifiers into it, pass that to sideMeal
                    sideMeal.put(item.getId(), quant);
                    KitchenMeal meal = new KitchenMeal(-1, employee.getId(), Tracker.getGson().toJson(mainMeal), Tracker.getGson().toJson(sideMeal), null, null);
                    InventoryConnection.connection().handleRequest("add-kitchen-meal", meal);
                    prop.put("success", true);
                    prop.put("message", "Success! Kitchen meal has been added.");
                }
                break;
        }
        return Tracker.getGson().toJson(prop);
    }
}
