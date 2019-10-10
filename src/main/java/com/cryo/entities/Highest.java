package com.cryo.entities;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Date;

@AllArgsConstructor
@Data
public class Highest {
    private final Date date;
    private final double price;
}
