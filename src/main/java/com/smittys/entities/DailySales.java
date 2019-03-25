package com.smittys.entities;

import com.smittys.Tracker;
import com.smittys.cache.CachedItem;
import com.smittys.db.impl.LabourConnection;
import com.smittys.utils.RoleNames;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ArrayUtils;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

@Data
@RequiredArgsConstructor
public class DailySales {

    private final Timestamp date;
    private final List<SalesItem> salesItems;

    private double labourCost;

    private double bohHours = -1, fohHours = -1, fohCost = -1, bohCost = -1;

    public void loadLabourCost() {
        CachedItem item = Tracker.getInstance().getCachingManager().get("labour-cost-cache");
        Object data = item.getCachedData("get-labour-cost-for-day", date);
        if (data == null) {
            this.labourCost = 0.0;
            return;
        }
        this.labourCost = (double) data;//(double) item.getCachedData("get-labour-cost-for-day", date);
    }

    public double getBOHCost() {
        if (bohCost < 0) loadHourData();
        return bohCost;
    }

    public double getFOHCost() {
        if (fohCost < 0) loadHourData();
        return fohCost;
    }

    public double getBOHHours() {
        if (bohHours < 0) loadHourData();
        return bohHours;
    }

    public double getFOHHours() {
        if (fohHours < 0) loadHourData();
        return fohHours;
    }

    public void loadHourData() {
        Object[] data = LabourConnection.connection().handleRequest("get-hours-for-day", date);
        SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy");
        if (data == null) return;
        ArrayList<HourData> hours = (ArrayList<HourData>) data[0];
        bohCost = hours.stream().filter(h -> ArrayUtils.contains(RoleNames.BOH, h.getRoleId())).mapToDouble(h -> h.getLength() * h.getWage()).sum();
        fohCost = hours.stream().filter(h -> ArrayUtils.contains(RoleNames.FOH, h.getRoleId())).mapToDouble(h -> h.getLength() * h.getWage()).sum();
        bohHours = hours.stream().filter(h -> ArrayUtils.contains(RoleNames.BOH, h.getRoleId())).mapToDouble(HourData::getLength).sum();
        fohHours = hours.stream().filter(h -> ArrayUtils.contains(RoleNames.FOH, h.getRoleId())).mapToDouble(HourData::getLength).sum();
    }

    public int getItemsSold() {
        return salesItems.stream().mapToInt(s -> s.getQuantity()).sum();
    }

    public double getTotalPrice() {
        return salesItems.stream().mapToDouble(s -> s.getQuantity() * s.getPrice()).sum();
    }
}
