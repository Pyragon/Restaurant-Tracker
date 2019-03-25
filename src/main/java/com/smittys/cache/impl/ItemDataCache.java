package com.smittys.cache.impl;

import com.smittys.cache.CachedItem;
import com.smittys.db.impl.InventoryConnection;
import com.smittys.entities.ItemData;

import java.util.HashMap;

public class ItemDataCache extends CachedItem {

    private HashMap<String, ItemData> itemData;
    private HashMap<String, Long> lastChecked;

    public ItemDataCache() {
        super("item-data-cache");
        itemData = new HashMap<>();
        lastChecked = new HashMap<>();
    }

    @Override
    public void fetchNewData(Object... data) {
        String code = (String) data[0];
        data = InventoryConnection.connection().handleRequest("get-item-by-code", code);
        if (data == null) return;
        ItemData item = (ItemData) data[0];
        itemData.put(code, item);
        lastChecked.put(code, System.currentTimeMillis());
    }

    @Override
    public void clear() {
        lastChecked = new HashMap<>();
    }

    @Override
    public long getCacheTimeLimit() {
        return -1;
    }

    @Override
    public Object getCachedData(Object... data) {
        String code = (String) data[0];
        boolean expired = hasExpired(data);
        if (expired) fetchNewData(data);
        if (!itemData.containsKey(code)) return null;
        return itemData.get(code);
    }

    private boolean hasExpired(Object... data) {
        String code = (String) data[0];
        if (!lastChecked.containsKey(code)) return true;
        return System.currentTimeMillis() - lastChecked.get(code) >= (5 * 60 * 1000);
    }
}
