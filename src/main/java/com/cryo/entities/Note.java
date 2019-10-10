package com.cryo.entities;

import lombok.Data;

import java.sql.Timestamp;

@Data
public class Note {

    private final int id;
    private final Timestamp date;
    private final String note;
    private final Timestamp added;

    public Object[] data() {
        return new Object[]{"DEFAULT", date, note, "DEFAULT"};
    }
}
