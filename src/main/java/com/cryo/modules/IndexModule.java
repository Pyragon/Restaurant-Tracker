package com.cryo.modules;

import com.cryo.Tracker;
import com.cryo.cache.impl.LabourCostCache;
import com.cryo.cache.impl.MonthlyTrackerCache;
import com.cryo.cache.impl.SyscoTruckCache;
import com.cryo.entities.Error;
import spark.Request;
import spark.Response;

import java.util.HashMap;

public class IndexModule extends WebModule {

    @Override
    public String[] getEndpoints() {
        return new String[]{"GET", "/"};
    }

    @Override
    public Object decodeRequest(String endpoint, Request request, Response response) {
        if (!request.requestMethod().equals("GET")) return error("Index only allows GET requests.");
        HashMap<String, Object> model = new HashMap<>();
        model.put("labourErrors", Tracker.getInstance().getErrorManager().getErrors(Error.ErrorParent.LABOUR, 5));
        model.put("inventoryErrors", Tracker.getInstance().getErrorManager().getErrors(Error.ErrorParent.INVENTORY, 5));
        SyscoTruckCache truckCache = (SyscoTruckCache) Tracker.getInstance().getCachingManager().get("sysco-truck-cache");
        MonthlyTrackerCache monthlyTrackerCache = (MonthlyTrackerCache) Tracker.getInstance().getCachingManager().get("monthly-tracker-cache");
        LabourCostCache labourCostCache = (LabourCostCache) Tracker.getInstance().getCachingManager().get("labour-cost-cache");
        model.put("truckCache", truckCache);
        model.put("monthlyTrackerCache", monthlyTrackerCache);
        model.put("labourCostCache", labourCostCache);
        model.put("index", true);
        model.put("redirect", request.queryParams("redirect"));
        return render("./source/modules/index/index.jade", model, request, response);
    }
}
