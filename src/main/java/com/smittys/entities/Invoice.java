package com.smittys.entities;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.sql.Timestamp;

@Data
@RequiredArgsConstructor
public class Invoice {

    private final int id;
    private final long invoiceId;
    private final String vendor;
    private final int quantityOrdered, quantityShipped;
    private final double price;
    private final Timestamp shipDate, dateEntered, lastUpdated;

    public void loadItems() {

    }

    public Object[] data() {
        return new Object[]{"DEFAULT", invoiceId, vendor, quantityOrdered, quantityShipped, price, shipDate, "DEFAULT", "DEFAULT"};
    }

}
