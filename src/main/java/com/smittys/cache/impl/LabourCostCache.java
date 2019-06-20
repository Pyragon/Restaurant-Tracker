package com.smittys.cache.impl;

import com.smittys.cache.CachedItem;
import com.smittys.db.impl.InventoryConnection;
import com.smittys.db.impl.LabourConnection;
import com.smittys.entities.Employee;
import com.smittys.entities.HourData;
import com.smittys.entities.SalesItem;

import java.sql.Timestamp;
import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class LabourCostCache extends CachedItem {

    private HashMap<String, Object> values;
    private HashMap<String, Long> lastChecked;

    private HashMap<String, Double> labourCosts;

    public LabourCostCache() {
        super("labour-cost-cache");
        values = new HashMap<>();
        lastChecked = new HashMap<>();
        labourCosts = new HashMap<>();
    }

    @Override
    public void fetchNewData(Object... data) {
        String opcode = (String) data[0];
        Object value = null;
        final Calendar cal = Calendar.getInstance();
        if (labourOudated()) fetchLabourCosts();
        SimpleDateFormat format = new SimpleDateFormat("MM-dd-yyyy");
        switch (opcode) {
            case "get-labour-cost-for-day":
                Date date = (Date) data[1];
                String formatted = format.format(date);
                value = labourCosts.containsKey(formatted) ? labourCosts.get(formatted) : 0.0;
                break;
            case "get-average-labour-cost-for-month":
            case "get-average-labour-cost-for-year":
                boolean year = opcode.contains("year");
                OptionalDouble op = labourCosts.keySet().stream().filter(c -> {
                    cal.setTime(new Date(System.currentTimeMillis()));
                    int month = cal.get(year ? Calendar.YEAR : Calendar.MONTH);
                    try {
                        cal.setTime(format.parse(c));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    return month == cal.get(year ? Calendar.YEAR : Calendar.MONTH);
                }).mapToDouble(c -> labourCosts.get(c)).average();
                if (op.isPresent()) value = op.getAsDouble();
                else value = 0;
                break;
            case "get-lowest-labour-cost-for-month":
            case "get-lowest-labour-cost-for-year":
            case "get-highest-labour-cost-for-month":
            case "get-highest-labour-cost-for-year":
                year = opcode.contains("year");
                boolean highest = opcode.contains("highest");
                Optional<String> opS = labourCosts.keySet().stream().filter(c -> {
                    cal.setTime(new Date(System.currentTimeMillis()));
                    int month = cal.get(year ? Calendar.YEAR : Calendar.MONTH);
                    try {
                        cal.setTime(format.parse(c));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    return month == cal.get(year ? Calendar.YEAR : Calendar.MONTH);
                }).sorted((s, s1) -> labourCosts.get(s) < labourCosts.get(s1) ? highest ? 1 : -1 : highest ? -1 : 1).findFirst();
                if (!opS.isPresent()) value = null;
                else {
                    try {
                        value = new Object[]{new Timestamp(format.parse(opS.get()).getTime()), labourCosts.get(opS.get())};
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
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

    public boolean labourOudated() {
        if (!lastChecked.containsKey("labour-costs")) return true;
        return System.currentTimeMillis() - lastChecked.get("labour-costs") >= (5 * 60 * 1000);
    }

    public void fetchLabourCosts() {
        HashMap<String, Double> labourCost = new HashMap<>();
        Object[] data = LabourConnection.connection().handleRequest("get-hours-for-year");
        if (data == null) return;
        ArrayList<HourData> hours = (ArrayList<HourData>) data[0];
        SimpleDateFormat format = new SimpleDateFormat("MM-dd-yyyy");
        hours.forEach(h -> {
            String date = format.format(h.getDate());
            if (!labourCost.containsKey(date)) labourCost.put(date, 0.0);
            double totalCost = labourCost.get(date);
            Object[] eData = LabourConnection.connection().handleRequest("get-employee", h.getEmployeeId());
            if (eData == null) return;
            Employee employee = (Employee) eData[0];
            totalCost += h.getLength() * employee.getWage();
            labourCost.put(date, totalCost);
        });
        labourCost.forEach((date, length) -> {
            //GET SALES FOR DAY
            try {
                Object[] salesData = InventoryConnection.connection().handleRequest("get-sales-items-by-date", new Timestamp(format.parse(date).getTime()));
                if (salesData == null) return;
                ArrayList<SalesItem> items = (ArrayList<SalesItem>) salesData[0];
                if (items.size() == 0) return;
                double totalSales = items.stream().mapToDouble(i -> i.getPrice() * i.getQuantity()).sum();
                double labourPercentage = length / totalSales;
                this.labourCosts.put(date, labourPercentage * 100);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        });
        lastChecked.put("labour-costs", System.currentTimeMillis());
    }

    @Override
    public long getCacheTimeLimit() {
        return -1;
    }

    @Override
    public Object getCachedData(Object... data) {
        //GET DATE FROM data INSTEAD OF STRING FOR OPCODE
        String opcode = (String) data[0];
        boolean expired = true;
        if (opcode.equals("get-labour-cost-for-day")) {
            Date stamp = (Date) data[1];
            SimpleDateFormat format = new SimpleDateFormat("MM-dd-yyyy");
            String date = format.format(stamp);
            expired = hasExpired(date);
            if (expired) fetchNewData(data);
            if (!labourCosts.containsKey(date)) {
                return null;
            }
            return labourCosts.get(date);
        }
        expired = hasExpired(data);
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
