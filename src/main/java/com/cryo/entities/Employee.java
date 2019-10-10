package com.cryo.entities;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.sql.Timestamp;

@RequiredArgsConstructor
@Data
public class Employee {

    private final int id;
    private final String firstName;
    private final String lastName;
    private final Timestamp startDate;
    private double wage;
    private Timestamp endDate;
    private final int defaultRole;

    private final Timestamp lastUpdated;

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public Object[] getData() {
        return new Object[]{"DEFAULT", firstName, lastName, startDate, endDate != null ? endDate : "NULL", defaultRole, wage, "DEFAULT"};
    }
}
