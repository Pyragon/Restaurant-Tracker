package com.smittys.entities;

import com.google.gson.internal.LinkedTreeMap;
import com.smittys.Tracker;
import com.smittys.db.impl.InventoryConnection;
import lombok.Data;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Properties;

@Data
public class RecipeItem {

    private final int id;
    private String name, billName, recipe, modifiers;
    private double price;
    private Timestamp added;

    private double cost;

    //yield = % of box required to be wasted.
    //yield calculated at overview (counts)
    //amount used = unit + (unit * yield)
    //i.e. 1oz is 1% of box, yield is !% of box
    //amount used for 1 oz = (.01 + (.01*.01))
    //amount used = 1.01 per oz
    //100 * oz = 101 (100% of box + 1% for the yield)

    public RecipeItem(int id, String name, String billName, String recipe, String modifiers, double price, Timestamp added) {
        this.id = id;
        this.name = name;
        this.billName = billName;
        this.recipe = recipe;
        this.modifiers = modifiers;
        this.price = price;
        this.added = added;
        this.cost = getCostToMake();
    }

    public Object[] data() {
        return new Object[]{"DEFAULT", name, billName, recipe, modifiers == null ? "NULL" : modifiers, price, "DEFAULT"};
    }

    public HashMap<String, Properties> getRecipeMap() {
        return Tracker.getGson().fromJson(this.recipe, HashMap.class);
    }

    public double getCostToMake() {
        boolean approx = false;
        HashMap<String, Properties> recipe = getRecipeMap();
        if (recipe == null) return 0.0;
        double cost = 0;
        for (Object key : recipe.keySet()) {
            Object value = recipe.get(key);
            Object data = Tracker.getInstance().getCachingManager().get("item-data-cache").getCachedData(key);
            LinkedTreeMap<String, Object> map = (LinkedTreeMap<String, Object>) value;
            if (data == null) {
                Object[] rData = InventoryConnection.connection().handleRequest("get-recipe-item-by-name", key);
                if (rData == null) continue;
                RecipeItem item = (RecipeItem) rData[0];
                //have 'units' in recipe_item as well, = recipe_item[unit]*quantity
                //return full item * quantity for now
                return item.getCostToMake() * ((double) map.get("quantity"));
            }
            ItemData item = (ItemData) data;
            cost += item.getPriceForUnit((double) map.get("quantity"), (String) map.get("unit"));
        }
        return cost;//new Object[]{approx, cost}; //cache?
    }

    public double getProfit() {
        return price - cost;
    }
}
