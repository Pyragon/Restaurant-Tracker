package com.smittys.entities;

import com.smittys.db.impl.InventoryConnection;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.text.DecimalFormat;

@Data
@RequiredArgsConstructor
public class InvoiceItem {

    private final int id;
    private final long invoiceId;
    private final String itemCode;
    private final double price;
    @MySQLRead("ext_price")
    private final double EXTPrice;
    private final int quantityOrdered, quantityShipped;
    private final String shipUnit, status;

    public String getPriceString() {
        DecimalFormat format = new DecimalFormat();
        format.setMaximumFractionDigits(2);
        format.setMinimumFractionDigits(2);
        return format.format(price);
    }

    public String getExtPriceString() {
        DecimalFormat format = new DecimalFormat();
        format.setMaximumFractionDigits(2);
        format.setMinimumFractionDigits(2);
        return format.format(EXTPrice);
    }

    public String getName() {
        ItemData data = InventoryConnection.connection().selectClass("item_data", "item_code=?", ItemData.class, itemCode);
        if(data == null) return "N/A";
        return data.getItemName();
    }

    public Object[] data() {
        return new Object[] { "DEFAULT", invoiceId,  itemCode, price, EXTPrice, quantityOrdered, quantityShipped, shipUnit, status };
    }
}
