package com.cryo.entities;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.sql.Timestamp;

@Data
@RequiredArgsConstructor
public class MiscData {

    private final int id;
    private final String key;
    private final Object value;
    private final Timestamp added;
}
