package com.cryo.entities;

import com.cryo.Tracker;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.sql.Timestamp;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Data
@RequiredArgsConstructor
public class ItemData {

    private final int id;
    private final String itemCode, upcCode;
    private final double price;
    private final String itemName, displayName, brand, vendor;
    private final double packCount, packSize;
    private final String packUnit, units;

    private final Timestamp added, lastUpdated;

    public Object[] data() {
        return new Object[]{"DEFAULT", itemCode, upcCode, price, itemName, displayName, brand, vendor, packCount, packSize, packUnit, "DEFAULT", "DEFAULT", "DEFAULT"};
    }

    public Properties getUnitProps() {
        Properties prop = new Properties();
        if (units == null || units.equals("")) return prop;
        prop = Tracker.getGson().fromJson(units, Properties.class);
        return prop;
    }

    public Object getRealPackCount() {
        if (packCount % 1 != 0) return packCount;
        return (int) packCount;
    }

    public Object getRealPackSize() {
        if (packSize % 1 != 0) return packSize;
        return (int) packSize;
    }

    public double getPriceForUnit(double quantity, String unit) {
        if (units == null || units.equals("")) {
            Tracker.getInstance().getErrorManager().add(new UnitError("missing-unit-" + id, "Missing unit " + unit + " for " + itemName, unit, id));
            return 0.0;
        }
        Properties prop = Tracker.getGson().fromJson(units, Properties.class);
        if (!prop.containsKey(unit)) {
            Tracker.getInstance().getErrorManager().add(new UnitError("missing-unit-" + id, "Missing unit " + unit + " for " + itemName, unit, id));
            return 0.0;
        }
        String multString = prop.getProperty(unit);
        if (multString.contains(" ")) {
            String[] spl = multString.split(" ");
            String quantString = spl[0];
            try {
                double quant = Double.parseDouble(quantString);
                String nUnit = Stream.of(spl).skip(1).collect(Collectors.joining(" "));
                return getPriceForUnit(quant, nUnit) * quantity;
            } catch (Exception e) {
                e.printStackTrace();
                return 0.0;
            }
        }
        double mult = Double.parseDouble(prop.getProperty(unit));
        return (mult * price) * quantity;
    }

    //{"ea": ".75"}
}
