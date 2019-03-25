package com.smittys.modules.labour;

import com.smittys.Tracker;
import com.smittys.db.impl.LabourConnection;
import com.smittys.entities.DailySales;
import com.smittys.entities.WebSection;
import com.smittys.modules.WebModule;
import spark.Request;
import spark.Response;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

public class SalesSection implements WebSection {

    @Override
    public String getName() {
        return "sales";
    }

    @Override
    public String decode(String action, Request request, Response response) {
        Properties prop = new Properties();
        HashMap<String, Object> model = new HashMap<>();
        switch (action) {
            case "load":
                String html = null;
                try {
                    html = WebModule.render("./source/modules/labour/sales/sales.jade", model, request, response);
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
                Object[] data = LabourConnection.connection().handleRequest("get-all-sales-days", page);
                if (data == null) {
                    prop.put("success", false);
                    prop.put("error", "Error loading sales.");
                    break;
                }
                ArrayList<Timestamp> stamps = (ArrayList<Timestamp>) data[0];
                ArrayList<DailySales> dailySales = new ArrayList<>(stamps.size());
                stamps.forEach(t -> {
                    SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy");
                    Object salesData = Tracker.getInstance().getCachingManager().get("daily-sales-cache").getCachedData(format.format(t));
                    dailySales.add((DailySales) salesData);
                });
                data = LabourConnection.connection().handleRequest("get-all-sales-days-count");
                model.put("daily", dailySales);
                prop.put("success", true);
                prop.put("html", WebModule.render("./source/modules/labour/sales/sales_list.jade", model, request, response));
                prop.put("pageTotal", data[0]);
                break;
            default:
                prop.put("success", false);
                prop.put("error", "Invalid action.");
                break;
        }
        return Tracker.getGson().toJson(prop);
    }
}
