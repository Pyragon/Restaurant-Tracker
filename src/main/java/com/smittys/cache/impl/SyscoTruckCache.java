package com.smittys.cache.impl;

import com.smittys.cache.CachedItem;
import com.smittys.db.impl.InventoryConnection;

import java.util.HashMap;

public class SyscoTruckCache extends CachedItem {

    private HashMap<String, Object> values;
    private HashMap<String, Long> lastChecked;

    public SyscoTruckCache() {
        super("sysco-truck-cache");
        values = new HashMap<>();
        lastChecked = new HashMap<>();
    }

    @Override
    public void fetchNewData(Object... data) {
        String opcode = (String) data[0];
        Object value = null;
        switch (opcode) {
            case "last-truck":
            case "next-truck":
                data = InventoryConnection.connection().handleRequest("get-" + opcode);
                value = data == null ? null : data[0];
                break;
            case "biggest-truck-this-month":
                data = InventoryConnection.connection().handleRequest("get-biggest-truck-this-month");
                value = data == null ? null : data[0];
                break;
            case "total-orders":
                data = InventoryConnection.connection().handleRequest("get-total-orders");
                value = data == null ? null : data[0];
                break;
            case "total-orders-price":
                data = InventoryConnection.connection().handleRequest("get-total-orders-price");
                value = data == null ? null : data[0];
                break;
        }
        if (value == null) return;
        lastChecked.put(opcode, System.currentTimeMillis());
        values.put(opcode, value);
    }

    @Override
    public void clear() {
        lastChecked = new HashMap<>();
    }

    @Override
    public Object getCachedData(Object... data) {
        String opcode = (String) data[0];
        boolean expired = hasExpired(data);
        if (expired) fetchNewData(data);
        if (!values.containsKey(opcode)) return null;
        return values.get(opcode);
    }

    private boolean hasExpired(Object... data) {
        String opcode = (String) data[0];
        if (!lastChecked.containsKey(opcode)) return true;
        return System.currentTimeMillis() - lastChecked.get(opcode) >= (5 * 60 * 1000);
    }

    @Override
    public long getCacheTimeLimit() {
        return -1;
    }
}
