package com.cryo.cache.impl;

import com.cryo.cache.CachedItem;
import com.cryo.db.impl.InventoryConnection;
import com.cryo.entities.Highest;
import com.cryo.entities.SalesItem;

import java.sql.Timestamp;
import java.util.*;

public class MonthlyTrackerCache extends CachedItem {

    private HashMap<String, Object> values;
    private HashMap<String, Long> lastChecked;

    public MonthlyTrackerCache() {
        super("monthly-tracker-cache");
        values = new HashMap<>();
        lastChecked = new HashMap<>();
    }

    @Override
    public void fetchNewData(Object... data) {
        String opcode = (String) data[0];
        Object value = null;
        final Calendar cal = Calendar.getInstance();
        switch (opcode) {
            case "current-sales":
                data = InventoryConnection.connection().handleRequest("get-sales-by-month", new Timestamp(new Date().getTime()));
                if (data == null) return;
                ArrayList<SalesItem> items = (ArrayList<SalesItem>) data[0];
                value = items.stream().mapToDouble(s -> s.getQuantity() * s.getPrice()).sum();
                break;
            case "highest-day-this-month":
                data = InventoryConnection.connection().handleRequest("get-sales-by-month", new Timestamp(new Date().getTime()));
                if (data == null) return;
                items = (ArrayList<SalesItem>) data[0];
                HashMap<Integer, Double> prices = new HashMap<>();
                items.forEach(s -> {
                    cal.setTime(s.getDate());
                    if (!prices.containsKey(cal.get(Calendar.DAY_OF_MONTH)))
                        prices.put(cal.get(Calendar.DAY_OF_MONTH), 0.0);
                    double price = prices.get(cal.get(Calendar.DAY_OF_MONTH));
                    prices.put(cal.get(Calendar.DAY_OF_MONTH), price + (s.getPrice() * s.getQuantity()));
                });
                Optional<Integer> day = prices.keySet().stream().sorted((d1, d2) -> prices.get(d1) > prices.get(d2) ? -1 : 1).findFirst();
                if (!day.isPresent()) {
                    value = null;
                    break;
                }
                cal.set(Calendar.DAY_OF_MONTH, day.get());
                value = new Highest(cal.getTime(), prices.get(day.get()));
                break;
            case "highest-day-this-year":
                data = InventoryConnection.connection().handleRequest("get-sales-by-month", new Timestamp(new Date().getTime()));
                if (data == null) return;
                items = (ArrayList<SalesItem>) data[0];
                HashMap<String, Double> yearPrices = new HashMap<>();
                items.forEach(s -> {
                    cal.setTime(s.getDate());
                    if (!yearPrices.containsKey(cal.get(Calendar.MONTH) + "-" + cal.get(Calendar.DAY_OF_MONTH)))
                        yearPrices.put(cal.get(Calendar.MONTH) + "-" + cal.get(Calendar.DAY_OF_MONTH), 0.0);
                    double price = yearPrices.get(cal.get(Calendar.MONTH) + "-" + cal.get(Calendar.DAY_OF_MONTH));
                    yearPrices.put(cal.get(Calendar.MONTH) + "-" + cal.get(Calendar.DAY_OF_MONTH), price + (s.getPrice() * s.getQuantity()));
                });
                Optional<String> key = yearPrices.keySet().stream().sorted((d1, d2) -> yearPrices.get(d1) > yearPrices.get(d2) ? -1 : 1).findFirst();
                if (!key.isPresent()) {
                    value = null;
                    break;
                }
                String[] spl = key.get().split("-");
                cal.set(Calendar.MONTH, Integer.parseInt(spl[0]));
                cal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(spl[1]));
                value = new Highest(cal.getTime(), yearPrices.get(cal.get(Calendar.MONTH) + "-" + cal.get(Calendar.DAY_OF_MONTH)));
                break;
            case "projected-sales":
                data = InventoryConnection.connection().handleRequest("get-sales-by-month", new Timestamp(new Date().getTime()));
                if (data == null) return;
                items = (ArrayList<SalesItem>) data[0];
                prices = new HashMap<>();
                items.forEach(s -> {
                    cal.setTime(s.getDate());
                    if (!prices.containsKey(cal.get(Calendar.DAY_OF_MONTH)))
                        prices.put(cal.get(Calendar.DAY_OF_MONTH), 0.0);
                    double price = prices.get(cal.get(Calendar.DAY_OF_MONTH));
                    prices.put(cal.get(Calendar.DAY_OF_MONTH), price + (s.getPrice() * s.getQuantity()));
                });
                OptionalDouble optionalDouble = prices.values().stream().mapToDouble(Double::valueOf).average();
                if (!optionalDouble.isPresent()) {
                    value = null;
                    break;
                }
                double average = optionalDouble.getAsDouble();
                double total = items.stream().mapToDouble(s -> s.getQuantity() * s.getPrice()).sum();
                int max = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
                int left = max - cal.get(Calendar.DAY_OF_MONTH);
                value = total + (average * left);
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
    public long getCacheTimeLimit() {
        return -1;
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
}
