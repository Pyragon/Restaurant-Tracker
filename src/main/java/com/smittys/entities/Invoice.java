package com.smittys.entities;

import com.smittys.db.impl.InventoryConnection;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.sql.Timestamp;
import java.util.ArrayList;

@Data
@RequiredArgsConstructor
public class Invoice {

    private final int id;
    private final long invoiceId;
    private final String vendor;
    private final int quantityOrdered, quantityShipped;
    private final double subtotal;
    @MySQLRead("gst")
    private final double GST;
    @MySQLRead("pst")
    private final double PST;
    private final double discount;
    private final Timestamp shipDate, dateEntered, lastUpdated;

    public ArrayList<InvoiceItem> getItems() {
        return InventoryConnection.connection().selectList("invoice_items", "invoice_id=?", InvoiceItem.class, invoiceId);
    }

    public double getASubtotal() {
        return subtotal+discount;
    }

    public double getTotal() {
        return subtotal+GST+PST+discount;
    }

    public Object[] data() {
        return new Object[]{"DEFAULT", invoiceId, vendor, quantityOrdered, quantityShipped, subtotal, GST, PST, discount, shipDate, "DEFAULT", "DEFAULT"};
    }

}
