package com.smittys.entities;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.sql.Timestamp;

@RequiredArgsConstructor
@Data
public class MonthlyHourData {

    private Timestamp month;
    private double fohHours;
    private double bohHours;

}
