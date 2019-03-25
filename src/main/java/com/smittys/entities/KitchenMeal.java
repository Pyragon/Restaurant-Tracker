package com.smittys.entities;

import com.smittys.Tracker;
import com.smittys.db.impl.InventoryConnection;
import com.smittys.db.impl.LabourConnection;
import lombok.RequiredArgsConstructor;

import java.sql.Timestamp;
import java.util.Properties;

@RequiredArgsConstructor
public class KitchenMeal {

    private final int id, employeeId;
    private final String main, side;
    private final Timestamp date, lastUpdated;

    public Object[] data() {
        return new Object[]{"DEFAULT", employeeId, main, side, "DEFAULT", "DEFAULT"};
    }

    public double getCostToMake() {
        double totalCost = 0.0;
        if (main != null && !main.equals("")) {
            Properties mainMeal = Tracker.getGson().fromJson(main, Properties.class);
            if (mainMeal != null) {
                for (Object obj : mainMeal.keySet()) {
                    int mealId = Integer.parseInt((String) obj);
                    Object[] data = InventoryConnection.connection().handleRequest("get-recipe-item-by-id", mealId);
                    if (data != null) {
                        RecipeItem item = (RecipeItem) data[0];
                        //TODO - change to get another JSON Object from mainMeal, which will contain both quantity and modifiers objects
                        if (mainMeal.get(mealId) != null && (mainMeal.get(mealId) instanceof String) && !mainMeal.get(mealId).equals("")) {
                            Properties quant = Tracker.getGson().fromJson((String) mainMeal.get(mealId), Properties.class);
                            double quantity = Double.parseDouble(quant.getProperty("quantity"));
                            totalCost += item.getCostToMake() * quantity;
                            //TODO apply modifiers
                        }
                    }
                }
            }
        }
        if (side != null && !side.equals("")) {
            Properties sideMeal = Tracker.getGson().fromJson(side, Properties.class);
            if (sideMeal != null) {
                for (Object obj : sideMeal.keySet()) {
                    String mealId = (String) obj;
                    Object[] data = InventoryConnection.connection().handleRequest("get-recipe-item-by-id", mealId);
                    if (data != null) {
                        RecipeItem item = (RecipeItem) data[0];
                        if (sideMeal.get(mealId) != null && (sideMeal.get(mealId) instanceof String) && !sideMeal.get(mealId).equals("")) {
                            Properties quant = Tracker.getGson().fromJson((String) sideMeal.get(mealId), Properties.class);
                            double quantity = Double.parseDouble(quant.getProperty("quantity"));
                            totalCost += item.getCostToMake() * quantity;
                            //TODO apply modifiers
                        }
                    }
                }
            }
        }
        return totalCost;
    }

    public String getEmployeeName() {
        Object[] data = LabourConnection.connection().handleRequest("get-employee", employeeId);
        if (data == null) return "Error";
        return ((Employee) data[0]).getFullName();
    }

}
