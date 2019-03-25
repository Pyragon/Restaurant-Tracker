package com.smittys.modules.inventory;

import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.reflect.TypeToken;
import com.smittys.Tracker;
import com.smittys.db.impl.InventoryConnection;
import com.smittys.entities.InventoryError;
import com.smittys.entities.RecipeItem;
import com.smittys.entities.WebSection;
import com.smittys.modules.WebModule;
import spark.Request;
import spark.Response;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.smittys.modules.WebModule.error;

public class RecipeItemModule implements WebSection {
    @Override
    public String getName() {
        return "recipe-items";
    }

    @Override
    public String decode(String action, Request request, Response response) {
        HashMap<String, Object> model = new HashMap<>();
        Properties prop = new Properties();
        switch (action) {
            case "load":
                String html;
                try {
                    html = WebModule.render("./source/modules/inventory/items/recipe_items.jade", model, request, response);
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
                Object[] data = InventoryConnection.connection().handleRequest("get-recipe-items", page);
                if (data == null) {
                    prop.put("success", false);
                    prop.put("error", "Error loading recipe items.");
                    break;
                }
                ArrayList<RecipeItem> hours = (ArrayList<RecipeItem>) data[0];
                model.put("recipeitems", hours);
                data = InventoryConnection.connection().handleRequest("get-recipe-items-count");
                if (data == null) {
                    prop.put("success", false);
                    prop.put("error", "Error getting page total.");
                    break;
                }
                prop.put("success", true);
                prop.put("html", WebModule.render("./source/modules/inventory/items/recipe_items_list.jade", model, request, response));
                prop.put("pageTotal", data[0]);
                break;
            case "modifiers-helper":
                return WebModule.render("./source/modules/inventory/items/modifiers_json_helper.jade", new HashMap<>(), request, response);
            case "view-add":
                prop.put("success", true);
                prop.put("html", WebModule.render("./source/modules/inventory/items/missing_recipe_item.jade", new HashMap<>(), request, response));
                break;
            case "add":
                return addRecipeItem(request.queryParams("itemName"), request.queryParams("billName"), request.queryParams("price"), request.queryParams("recipe"), request.queryParams("modifiers"));
            case "remove":
                String idString = request.queryParams("id");
                if (idString == null) {
                    prop.put("success", "false");
                    prop.put("error", "Invalid ID");
                    break;
                }
                int id = Integer.parseInt(idString);
                InventoryConnection.connection().handleRequest("remove-recipe-item", id);
                prop.put("success", true);
                break;
            case "edit":
                if (request.requestMethod().equals("GET")) {
                    try {
                        idString = request.queryParams("id");
                        if (idString == null) {
                            html = WebModule.render("./source/modules/inventory/items/missing_recipe_item.jade", model, request, response);
                            prop.put("success", true);
                            prop.put("html", html);
                        } else {
                            id = Integer.parseInt(request.queryParams("id"));
                            data = InventoryConnection.connection().handleRequest("get-recipe-item-by-id", id);
                            if (data == null) return error("Unable to find that item.");
                            RecipeItem item = (RecipeItem) data[0];
                            if (item.getName() != null && !item.getName().equals(""))
                                model.put("item_name", item.getName());
                            if (item.getBillName() != null && !item.getBillName().equals(""))
                                model.put("bill_name", item.getBillName());
                            model.put("price", item.getPrice());
                            if (item.getRecipe() != null) {
                                HashMap<String, LinkedTreeMap<String, String>> rec = Tracker.getGson().fromJson(item.getRecipe(), HashMap.class);
                                model.put("recipe", item.getRecipe());
                            }
                            html = WebModule.render("./source/modules/inventory/items/missing_recipe_item.jade", model, request, response);
                            prop.put("success", true);
                            prop.put("html", html);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        return error("Error loading edit.");
                    }
                    break;
                }
                idString = request.queryParams("id");
                if (idString == null) return error("No id specified.");
                id = Integer.parseInt(idString);
                data = InventoryConnection.connection().handleRequest("get-recipe-item-by-id", id);
                if (data == null) return error("Error finding that item.");
                RecipeItem item = (RecipeItem) data[0];
                String itemName = request.queryParams("itemName");
                String billName = request.queryParams("billName");
                String priceString = request.queryParams("price");
                String recipeString = request.queryParams("recipe");
                ArrayList<String> toUpdate = new ArrayList<>();
                ArrayList<Object> values = new ArrayList<>();
                if (different(item.getName(), itemName)) item.setName(itemName);
                if (different(item.getBillName(), billName)) item.setBillName(billName);
                if (itemName != null && (item.getName() == null || !item.getName().equals(itemName))) {
                    item.setName(itemName);
                    toUpdate.add("name");
                    values.add(itemName);
                }
                if (billName != null && item.getBillName() == null || !item.getBillName().equals(billName)) {
                    item.setBillName(billName);
                    values.add(billName);
                    toUpdate.add("bill_name");
                }
                if (priceString == null) return error("Price must be filled out.");
                double price;
                try {
                    price = Double.parseDouble(priceString.replace("$", ""));
                } catch (Exception e) {
                    return error("Unable to parse price. Make sure it's a # of at least $0");
                }
                if (price < 0) return error("Price must be at least $0");
                if (price != item.getPrice()) {
                    item.setPrice(price);
                    values.add(price);
                    toUpdate.add("price");
                }
                if (recipeString == null) return error("Recipe must be specified.");
                Properties recipe = parseRecipe(recipeString, true);
                if (recipe.containsKey("errors")) {
                    prop.put("success", false);
                    prop.put("error", "Error parsing recipe.");
                    break;
                }
                String newRecipeString = Tracker.getGson().toJson(recipe);
                if (!item.getRecipe().equals(newRecipeString)) {
                    item.setRecipe(newRecipeString);
                    values.add(newRecipeString);
                    toUpdate.add("recipe");
                }
                if (values.size() <= 0) {
                    prop.put("success", false);
                    prop.put("error", "Nothing has changed.");
                    break;
                }
                InventoryConnection.connection().handleRequest("update-recipe-item", item.getId(), toUpdate.toArray(new String[toUpdate.size()]), values.toArray());
                prop.put("success", true);
                break;
        }
        return Tracker.getGson().toJson(prop);
    }

    public boolean different(String first, String second) {
        if ((first == null && second != null) || (first != null && second == null)) return true;
        return first.equals(second);
    }

    public static String addRecipeItem(String itemName, String billName, String priceString, String recipeString, String modifierString) {
        Properties prop = new Properties();
        double price = 0.0;
        if (priceString != null && !priceString.equals("")) {
            try {
                price = Double.parseDouble(priceString);
            } catch (Exception e) {
                return error("Price must be a number!");
            }
        }
        if (recipeString == null) return error("Price and recipe must be filled out!");
        if (itemName == null && billName == null) return error("Item name or bill name must be filled out!");
        if (price < 0) return error("Price must be at least $0.");
        Properties recipe = parseRecipe(recipeString, true);
        if (recipe.containsKey("errors")) {
            prop.put("success", true);
            prop.put("errors", recipe.get("errors"));
            return Tracker.getGson().toJson(prop);
        }
        Properties modifiers = parseRecipe(modifierString, true);
        if (modifiers != null && modifiers.containsKey("errors")) {
            prop.put("success", true);
            prop.put("errors", recipe.get("errors"));
            return Tracker.getGson().toJson(prop);
        }
        String modJson = modifiers == null ? "" : Tracker.getGson().toJson(modifiers);
        RecipeItem item = new RecipeItem(-1, itemName, billName, Tracker.getGson().toJson(recipe), modJson, price, null);
        InventoryConnection.connection().handleRequest("add-recipe-item", item);
        prop.put("success", true);
        prop.put("message", "Item has successfully been added.");
        return Tracker.getGson().toJson(prop);
    }

    public static Properties parseRecipe(String recipeString, boolean isArray) {
        TypeToken<?> token = new TypeToken<List<Properties>>() {
        };
        List<Properties> recipes = Tracker.getGson().fromJson(recipeString, token.getType());
        if (recipes == null) return null;
        Properties recipe = new Properties();
        ArrayList<String> errors = new ArrayList<>();
        recipes.forEach(r -> {
            try {
                if (r.getProperty("key").equals("")) return;
                Long.parseLong(r.getProperty("key"));
                Object[] conData = InventoryConnection.connection().handleRequest("get-item-by-code", r.getProperty("key"));
                if (conData == null) {
                    errors.add("No item found for " + r.getProperty("key"));
                    return;
                }
            } catch (Exception e) {
                String name = r.getProperty("key");
                if (name.startsWith("-")) {
                    errors.add("Adding custom choices is not supported yet.");
                    return;
                }
                Object conData = InventoryConnection.connection().handleRequest("get-recipe-item-by-name", name);
                if (conData == null) {
                    errors.add("Could not find item for " + name);
                    return;
                }
            }
            String value = r.getProperty("value");
            double quantity;
            String unit;
            try {
                String[] spl = value.split(" ");
                if (spl.length < 2) {
                    errors.add("Unable to parse quantity for " + r.getProperty("key"));
                    return;
                }
                quantity = Double.parseDouble(spl[0]);
                unit = Stream.of(spl).skip(1).collect(Collectors.joining(" "));
                InventoryError.AllowedUnits allowed = InventoryError.AllowedUnits.getUnit(unit);
                if (allowed == null) {
                    errors.add(unit + " is not currently supported as a unit.");
                    return;
                }
                unit = allowed.getUnits()[0];
            } catch (Exception e) {
                errors.add("Unable to parse quantity or unit for " + r.getProperty("key"));
                e.printStackTrace();
                return;
            }
            Properties recipeItem = new Properties();
            recipeItem.put("quantity", quantity);
            recipeItem.put("unit", unit);
            recipe.put(r.getProperty("key"), recipeItem);
        });
        if (errors.size() > 0) recipe.put("errors", errors);
        return recipe;
    }
}
