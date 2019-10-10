package com.cryo.utils;

import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Date;

import static org.parboiled.common.Preconditions.checkArgument;

public class NumberUtils {

    public String formatNumber(double number) {
        return formatNumber(number, "$");
    }

    public String formatNumber(double number, String symbol) {
        DecimalFormat format = new DecimalFormat(symbol + "#,##0.00");
        return format.format(number);
    }

    public String getDayOfMonthSuffix(Date date) {
        return getDayOfMonthSuffix(new Timestamp(date.getTime()));
    }

    public String getDayOfMonthSuffix(Timestamp timestamp) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(timestamp);
        int n = cal.get(Calendar.DAY_OF_MONTH);
        checkArgument(n >= 1 && n <= 31, "illegal day of month: " + n);
        if (n >= 11 && n <= 13) {
            return "th";
        }
        switch (n % 10) {
            case 1:
                return "st";
            case 2:
                return "nd";
            case 3:
                return "rd";
            default:
                return "th";
        }
    }
}
