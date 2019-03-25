package com.smittys.entities;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class InvoiceItem {

    private final int id;
    private final long invoiceId;
    private final String itemCode;
    private final double price, EXTPrice;
    private final int quantityOrdered, quantityShipped;
    private final String shipUnit, status;

    public Object[] data() {
        return new Object[] { "DEFAULT", invoiceId,  itemCode, price, EXTPrice, quantityOrdered, quantityShipped, shipUnit, status };
    }
}
