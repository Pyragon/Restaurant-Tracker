package com.smittys.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.sql.Timestamp;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
public class Modifier extends MySQLDao {

    @MySQLDefault
    private final int id;
    private final String name;
    private final double price;
    private final String recipe;
    @MySQLDefault
    private final Timestamp added;

    public double getCost() {
        return 0.0;
    }

}
