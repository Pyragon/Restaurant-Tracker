package com.smittys.cache.impl;

import com.smittys.cache.CachedItem;
import com.smittys.db.impl.InventoryConnection;
import com.smittys.entities.DailySales;
import com.smittys.entities.SalesItem;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.sql.Date;
import java.util.HashMap;

public class DailySalesCache extends CachedItem {

    private HashMap<String, Object> values;
    private HashMap<String, Long> lastChecked;

    public DailySalesCache() {
        super("daily-sales-cache");
        values = new HashMap<>();
        lastChecked = new HashMap<>();
    }

    @Override
    public void clear() {
        lastChecked = new HashMap<>();
    }

    @Override
    public void fetchNewData(Object... data) {
        String opcode = (String) data[0];
        SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy");
        Date date;
        try {
            date = new Date(format.parse(opcode).getTime());
        } catch (ParseException e) {
            e.printStackTrace();
            return;
        }
        Object[] salesData = InventoryConnection.connection().handleRequest("get-sales-items-by-date", date);
        if (salesData == null) {
            System.out.println("Error loading sales for: "+opcode);
            return;
        }
        ArrayList<SalesItem> items = (ArrayList<SalesItem>) salesData[0];
        if (items.size() == 0) {
            System.out.println("No sales items for: "+opcode);
            return;
        }
        DailySales dailySales = new DailySales(date, items);
        dailySales.loadLabourCost();
        values.put(opcode, dailySales);
        lastChecked.put(opcode, System.currentTimeMillis());
    }

    @Override
    public long getCacheTimeLimit() {
        return -1;
    }

    @Override
    public Object getCachedData(Object... data) {
        //GET DATE FROM data INSTEAD OF STRING FOR OPCODE
        String opcode = (String) data[0];
        boolean expired = hasExpired(data);
        if (expired) fetchNewData(data);
        if (!values.containsKey(opcode)) return null;
        return values.get(opcode);
    }

    private boolean hasExpired(Object... data) {
        String opcode = (String) data[0];
        if (!lastChecked.containsKey(opcode)) return true;
        return false;
    }
}
