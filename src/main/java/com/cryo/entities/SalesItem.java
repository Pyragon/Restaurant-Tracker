package com.cryo.entities;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.sql.Timestamp;

@AllArgsConstructor
@Data
public class SalesItem {

    private int id;
    private String billName;
    private int quantity;
    private double price;
    private Timestamp date;

    public Object[] data() {
        return new Object[]{"DEFAULT", billName, quantity, price, date};
    }
}
