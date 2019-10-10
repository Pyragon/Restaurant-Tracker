package com.cryo.entities;

import com.mysql.jdbc.StringUtils;
import com.cryo.Tracker;
import com.cryo.db.impl.InventoryConnection;
import com.cryo.managers.ErrorManager;
import com.cryo.modules.WebModule;
import com.cryo.modules.inventory.RecipeItemModule;
import lombok.Getter;
import spark.Request;
import spark.Response;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

import static com.cryo.modules.WebModule.error;

public class InventoryError extends Error {

    public enum AllowedUnits {
        EA("ea", "each"),
        FL_OZ("fl oz", "fl ozs", "fluid", "fluid ounce", "fluid ounces", "floz"),
        OZ("oz", "ounces", "ounce"),
        LB("lb", "pounds", "lbs", "pound"),
        GRAM("g", "grams", "gram"),
        SLICE("slice", "slices", "sl"),
        STRIP("strip", "strips"),
        TSP("tsp"),
        TBSP("tbsp"),
        PIECE("pc", "piece", "pieces"),
        WEDGE("wedge", "wedges"),
        LEAF("leaf");

        //{"ea": ".0053", "slice": "1 ea"}

        private @Getter
        String[] units;

        AllowedUnits(String... units) {
            this.units = units;
        }

        private static HashMap<String, AllowedUnits> allowedUnits;

        static {
            allowedUnits = new HashMap<>();
            Stream.of(AllowedUnits.values()).forEach(a -> {
                Stream.of(a.units).forEach(u -> allowedUnits.put(u, a));
            });
        }

        public static AllowedUnits getUnit(String unit) {
            return allowedUnits.containsKey(unit) ? allowedUnits.get(unit) : null;
        }

        public static boolean isAllowed(String unit) {
            return allowedUnits.containsKey(unit);
        }
    }

    public InventoryError(String key, String shortMessage, String longMessage) {
        super(-1, key, shortMessage, longMessage, ErrorParent.INVENTORY, true);
    }

    public InventoryError(int id, String key, String shortMessage, String longMessage, boolean active) {
        super(id, key, shortMessage, longMessage, ErrorParent.INVENTORY, active);
    }

    @Override
    public boolean hasLeftClick() {
        return key.toLowerCase().contains("missing-recipe-item") || key.toLowerCase().contains("missing-display");
    }

    @Override
    public boolean refreshAfterClick() {
        return true;
    }

    @Override
    public boolean opensModal() {
        return key.toLowerCase().contains("missing-recipe-item") || key.toLowerCase().contains("missing-display");
    }

    @Override
    public Properties getModalData(Request request, Response response) {
        if (!key.toLowerCase().contains("missing-recipe-item") && !key.toLowerCase().contains("missing-display"))
            return null;
        //html - String of modal
        //list of Properties for buttons
        //list includes class name and method-key to send to server on button press
        Properties prop = new Properties();
        prop.put("success", true);
        prop.put("html", loadModalTemplate(request, response));
        prop.put("buttons", getButtons());
        prop.put("title", "Missing " + (key.toLowerCase().contains("missing-display") ? "Display Name" : "Recipe Item"));
        return prop;
    }

    public String loadModalTemplate(Request request, Response response) {
        String keyS = key.toLowerCase();
        HashMap<String, Object> model = new HashMap<>();
        String fileName = null;
        if (keyS.contains("missing-recipe-item")) {
            String billName = key.replace("missing-recipe-item-", "");
            Object[] data = InventoryConnection.connection().handleRequest("get-recipe-item-by-bill-name", billName);
            if (data != null) model.put("price", ((RecipeItem) data[0]).getPrice());
            model.put("bill_name", billName);
            fileName = "./source/modules/inventory/items/missing_recipe_item.jade";
        } else if (keyS.contains("missing-display"))
            fileName = "./source/modules/inventory/items/missing_display_error.jade";
        if (fileName != null) return WebModule.render(fileName, model, request, response);
        return null;
    }

    public ArrayList<Properties> getButtons() {
        ArrayList<Properties> props = new ArrayList<>();
        if (key.toLowerCase().contains("missing-recipe-item") || key.toLowerCase().contains("missing-display")) {
            Properties submit = new Properties();
            submit.put("text", "Submit");
            submit.put("class", "btn btn-primary submit-btn");
            submit.put("key", key);
            submit.put("getData", true);
            props.add(submit);
            Properties close = new Properties();
            close.put("text", "Cancel");
            close.put("class", "btn btn-danger");
            close.put("key", key);
            close.put("closeOnClick", true);
            props.add(close);
        }
        return props;
    }

    @Override
    public boolean recheck() {
        if (key.toLowerCase().contains("missing-recipe-item")) {
            String name = key.replace("missing-recipe-item-", "");
            Object[] data = InventoryConnection.connection().handleRequest("get-recipe-item-by-bill-name", name);
            if (data == null) return true;
            RecipeItem item = (RecipeItem) data[0];
            return StringUtils.isNullOrEmpty(item.getRecipe()) || item.getRecipeMap().isEmpty();
        } else if (key.toLowerCase().contains("missing-display")) {
            String name = key.replace("missing-display-", "");
            Object[] data = InventoryConnection.connection().handleRequest("get-item-by-code", name);
            return data == null;
        }
        return true;
    }

    @Override
    public String buttonClick(String button, Properties data) {
        Properties prop = new Properties();
        if (key.toLowerCase().contains("missing-recipe-item"))
            return RecipeItemModule.addRecipeItem(data.getProperty("item-name"), data.getProperty("bill-name"), data.getProperty("price"), data.getProperty("recipe"), data.getProperty("modifiers"));
        if (key.toLowerCase().contains("missing-display")) {
            String displayName = data.getProperty("display-name");
            if (StringUtils.isNullOrEmpty(displayName)) return error("Display Name must be specified.");
            InventoryConnection.connection().handleRequest("set-display-name", key.replace("missing-display-", ""), displayName);
            prop.put("success", true);
            prop.put("message", "Display name has been successfully added.");
            return Tracker.getGson().toJson(prop);
        }
        prop.put("success", false);
        prop.put("error", "Cannot find action for that error.");
        return Tracker.getGson().toJson(prop);
    }

    @Override
    public String click() {
        ErrorManager manager = Tracker.getInstance().getErrorManager();
        manager.recheck(key);
        Properties prop = new Properties();
        ArrayList<Properties> retList = new ArrayList<>();
        List<Error> errorList = manager.getErrors(parent, 5);
        for (Error error : errorList) {
            Properties errorProp = new Properties();
            errorProp.put("key", error.getKey());
            errorProp.put("shortMessage", error.getShortMessage());
            errorProp.put("type", error.getType());
            errorProp.put("hasLeftClick", error.hasLeftClick());
            errorProp.put("refreshAfterClick", error.refreshAfterClick());
            errorProp.put("opensModal", error.opensModal());
            if (error.getLink() != null) errorProp.put("link", error.getLink());
            retList.add(errorProp);
        }
        prop.put("success", true);
        prop.put("errors", retList);
        return Tracker.getGson().toJson(prop);
    }
}
